/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.FirClassBuilder
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.psi.*

class RawFirFragmentForLazyBodiesBuilder<T : KtElement> private constructor(
    session: FirSession,
    baseScopeProvider: FirScopeProvider,
    private val rootNonLocalDeclaration: KtDeclaration,
    private val replacement: Pair<T, T>? = null
) : RawFirBuilder(session, baseScopeProvider, RawFirBuilderMode.NORMAL) {

    companion object {
        fun <T : KtElement> buildWithReplacement(
            session: FirSession,
            baseScopeProvider: FirScopeProvider,
            designation: List<FirDeclaration>,
            rootNonLocalDeclaration: KtDeclaration,
            replacement: Pair<T, T>? = null
        ): FirDeclaration {
            if (replacement != null) {
                require(replacement.first::class == replacement.second::class) {
                    "Build with replacement is possible for same type in replacements but given\n${replacement::class.simpleName} and ${replacement.second::class.simpleName}"
                }
            }
            val builder = RawFirFragmentForLazyBodiesBuilder(session, baseScopeProvider, rootNonLocalDeclaration, replacement)
            builder.context.packageFqName = rootNonLocalDeclaration.containingKtFile.packageFqName
            return builder.moveNext(designation.iterator())
        }

        fun build(
            session: FirSession,
            baseScopeProvider: FirScopeProvider,
            designation: List<FirDeclaration>,
            rootNonLocalDeclaration: KtDeclaration
        ): FirDeclaration {
            val builder = RawFirFragmentForLazyBodiesBuilder<KtElement>(session, baseScopeProvider, rootNonLocalDeclaration)
            builder.context.packageFqName = rootNonLocalDeclaration.containingKtFile.packageFqName
            return builder.moveNext(designation.iterator())
        }
    }

    private fun moveNext(iterator: Iterator<FirDeclaration>): FirDeclaration {
        if (!iterator.hasNext()) {
            val visitor = object : Visitor() {
                public override fun onConvert(element: KtElement): FirElement? =
                    super.onConvert(
                        if (replacement != null && replacement.first == element) replacement.second else element
                    )

                public override fun onConvertProperty(ktProperty: KtProperty, ownerClassBuilder: FirClassBuilder?): FirProperty {
                    if (replacement == null || replacement.first != ktProperty)
                        return super.onConvertProperty(ktProperty, ownerClassBuilder)

                    val second = replacement.second
                    check(second is KtProperty)
                    return super.onConvertProperty(second, ownerClassBuilder)
                }
            }

            //TODO Replace with convert
            return if (rootNonLocalDeclaration is KtProperty) {
                //TODO Non Top Level properties does not accepts correctly because of passing null instead of correct OwnerClassBuilder
                visitor.onConvertProperty(rootNonLocalDeclaration, null)
            } else {
                visitor.onConvert(rootNonLocalDeclaration)
            } as FirDeclaration
        }

        val parent = iterator.next()
        if (parent !is FirRegularClass) return moveNext(iterator)

        val classOrObject = parent.psi
        check(classOrObject is KtClassOrObject)

        withChildClassName(classOrObject.nameAsSafeName, false) {
            withCapturedTypeParameters {
                if (!parent.isInner) context.capturedTypeParameters = context.capturedTypeParameters.clear()
                addCapturedTypeParameters(parent.typeParameters.take(classOrObject.typeParameters.size))
                registerSelfType(classOrObject.toDelegatedSelfType(parent))
                return moveNext(iterator)
            }
        }
    }

    private fun PsiElement?.toDelegatedSelfType(firClass: FirRegularClass): FirResolvedTypeRef =
        toDelegatedSelfType(firClass.typeParameters, firClass.symbol)
}

