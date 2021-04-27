/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirLightSourceElement
import org.jetbrains.kotlin.fir.FirPsiSourceElement
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.psi.KtConstructorCalleeExpression
import org.jetbrains.kotlin.psi.KtTypeReference

interface SourceNavigator {

    fun FirTypeRef.isInConstructorCallee(): Boolean

    companion object {
        fun forElement(e: FirElement): SourceNavigator = when (e.source) {
            is FirLightSourceElement -> LightTreeSourceNavigator
            is FirPsiSourceElement<*> -> PsiSourceNavigator
            null -> PsiSourceNavigator //shouldn't matter
        }
    }
}

object PsiSourceNavigator : SourceNavigator {

    //Swallows incorrect casts!!!
    private inline fun <reified P : PsiElement> FirElement.psi(): P? {
        val psi = (source as? FirPsiSourceElement<*>)?.psi
        return psi as? P
    }

    override fun FirTypeRef.isInConstructorCallee(): Boolean = psi<KtTypeReference>()?.parent is KtConstructorCalleeExpression
}

object LightTreeSourceNavigator : SourceNavigator {

    private fun <T> FirElement.withSource(f: (FirLightSourceElement) -> T): T? =
        source?.let { f(it as FirLightSourceElement) }

    override fun FirTypeRef.isInConstructorCallee(): Boolean = withSource { source ->
        source.treeStructure.getParent(source.lighterASTNode)?.tokenType == KtNodeTypes.CONSTRUCTOR_CALLEE
    } ?: false
}
