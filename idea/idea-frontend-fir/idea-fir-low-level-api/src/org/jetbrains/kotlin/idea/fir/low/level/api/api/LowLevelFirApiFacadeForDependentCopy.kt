/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.api

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentsOfType
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.idea.fir.low.level.api.FirModuleResolveStateDepended
import org.jetbrains.kotlin.idea.fir.low.level.api.FirModuleResolveStateImpl
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirTowerContextProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirTowerDataContextCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirTowerDataContextInsideElementCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.file.structure.FirElementsRecorder
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.util.originalDeclaration
import org.jetbrains.kotlin.idea.util.getElementTextInContext
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

object LowLevelFirApiFacadeForDependentCopy {

    private fun KtDeclaration.canBeEnclosingDeclaration(): Boolean = when (this) {
        is KtNamedFunction -> isTopLevel || containingClassOrObject?.isLocal == false
        is KtProperty -> isTopLevel || containingClassOrObject?.isLocal == false
        is KtClassOrObject -> !isLocal
        is KtTypeAlias -> isTopLevel() || containingClassOrObject?.isLocal == false
        else -> false
    }

    fun findEnclosingNonLocalDeclaration(position: KtElement): KtNamedDeclaration? =
        position.parentsOfType<KtNamedDeclaration>().firstOrNull { ktDeclaration ->
            ktDeclaration.canBeEnclosingDeclaration()
        }

    private fun <T : KtElement> locateDeclarationInFileByOffset(offsetElement: T, file: KtFile): T? {
        val elementOffset = offsetElement.textOffset
        val elementAtOffset = file.findElementAt(elementOffset) ?: return null
        return PsiTreeUtil.getParentOfType(elementAtOffset, offsetElement::class.java, false)?.takeIf { it.textOffset == elementOffset }
    }

    private fun recordOriginalDeclaration(targetDeclaration: KtNamedDeclaration, originalDeclaration: KtNamedDeclaration) {
        require(!targetDeclaration.isPhysical)
        require(originalDeclaration.containingKtFile !== targetDeclaration.containingKtFile)
        val originalDeclrationParents = originalDeclaration.parentsOfType<KtDeclaration>().toList()
        val fakeDeclarationParents = targetDeclaration.parentsOfType<KtDeclaration>().toList()
        originalDeclrationParents.zip(fakeDeclarationParents) { original, fake ->
            fake.originalDeclaration = original
        }
    }

    private fun <T : KtElement> onAirResolveElement(
        state: FirModuleResolveState,
        place: T,
        elementToResolve: T,
    ): FirModuleResolveState {
        require(state is FirModuleResolveStateImpl)
        require(place.isPhysical)

        val collector = FirTowerDataContextInsideElementCollector(elementToResolve)
        val declaration = runBodyResolve(state, replacement = place to elementToResolve)

        val expressionLocator = object : FirVisitorVoid() {
            var result: FirElement? = null
                private set

            override fun visitElement(element: FirElement) {
                if (element.realPsi == elementToResolve) result = element
                if (result != null) return
                element.acceptChildren(this)
            }
        }
        declaration.accept(expressionLocator)

        val recordedMap = FirElementsRecorder.recordElementsFrom(declaration, FirElementsRecorder())
        return FirModuleResolveStateDepended(state, collector, recordedMap)
    }

    fun onAirGetTowerContextProvider(
        state: FirModuleResolveState,
        place: KtElement,
    ): FirTowerContextProvider {
        require(state is FirModuleResolveStateImpl)
        require(place.isPhysical)

        return FirTowerDataContextInsideElementCollector(place).also {
            runBodyResolve(state, collector = it, replacement = place to place)
        }
    }

    fun getResolveStateForDependedCopy(
        originalState: FirModuleResolveState,
        originalKtFile: KtFile,
        dependencyKtElement: KtElement
    ): FirModuleResolveState {
        require(originalState is FirModuleResolveStateImpl)

        val dependencyNonLocalDeclaration = findEnclosingNonLocalDeclaration(dependencyKtElement)
            ?: error("Cannot find enclosing declaration for ${dependencyKtElement.getElementTextInContext()}")

        val sameDeclarationInOriginalFile = locateDeclarationInFileByOffset(dependencyNonLocalDeclaration, originalKtFile)
            ?: error("Cannot find original function matching to ${dependencyNonLocalDeclaration.getElementTextInContext()} in $originalKtFile")

        recordOriginalDeclaration(
            targetDeclaration = dependencyNonLocalDeclaration,
            originalDeclaration = sameDeclarationInOriginalFile
        )

        val collector = FirTowerDataContextInsideElementCollector(dependencyKtElement)
        val copiedFirDeclaration = runBodyResolve(
            originalState,
            collector = collector,
            replacement = sameDeclarationInOriginalFile to dependencyNonLocalDeclaration
        )

        val recordedMap = FirElementsRecorder.recordElementsFrom(copiedFirDeclaration, FirElementsRecorder())

        return FirModuleResolveStateDepended(originalState, collector, recordedMap)
    }

    private fun <T : KtElement> runBodyResolve(
        state: FirModuleResolveStateImpl,
        replacement: Pair<T, T>,
        collector: FirTowerDataContextCollector? = null,
    ): FirDeclaration {
        val copiedFirDeclaration = DeclarationCopyBuilder.createDeclarationCopy(
            state = state,
            replacement = replacement,
        )

        val originalFirFile = state.getFirFile(replacement.first.containingKtFile)

        state.firFileBuilder.runCustomResolveWithPCECheck(originalFirFile, state.rootModuleSession.cache) {
            state.firLazyDeclarationResolver.runLazyResolveWithoutLock(
                copiedFirDeclaration,
                state.rootModuleSession.cache,
                originalFirFile,
                originalFirFile.session.firIdeProvider,
                fromPhase = copiedFirDeclaration.resolvePhase,
                toPhase = FirResolvePhase.BODY_RESOLVE,
                towerDataContextCollector = collector,
                checkPCE = true,
                lastNonLazyPhase = FirResolvePhase.IMPORTS
            )
        }

        return copiedFirDeclaration
    }

    fun FirModuleResolveState.getTowerContextProvider(): FirTowerContextProvider =
        require(this is FirModuleResolveStateDepended) {
            "Invalid resolve state ${this::class.simpleName} but have to be ${FirModuleResolveStateDepended::class.simpleName}"
        }.let { towerContextProvider }
}
