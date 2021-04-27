/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirLightSourceElement
import org.jetbrains.kotlin.fir.FirPsiSourceElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.expressions.FirStatement

//todo all the hustle just to make it fit into current checkers hierarchy with minimum diff
//probably better solution would be to introduce base checker interface
interface FirSyntaxChecker<in D : FirElement, P : PsiElement> {

    fun checkSyntax(element: D, context: CheckerContext, reporter: DiagnosticReporter) {
        val source = element.source ?: return
        if (!isApplicable(element, source)) return
        @Suppress("UNCHECKED_CAST")
        when (source) {
            is FirPsiSourceElement<*> -> checkPsi(element, source as FirPsiSourceElement<P>, context, reporter)
            is FirLightSourceElement -> checkLightTree(element, source, context, reporter)
        }
    }

    fun isApplicable(element: D, source: FirSourceElement): Boolean = true

    fun checkPsi(element: D, source: FirPsiSourceElement<P>, context: CheckerContext, reporter: DiagnosticReporter)

    fun checkLightTree(element: D, source: FirLightSourceElement, context: CheckerContext, reporter: DiagnosticReporter)
}

abstract class FirDeclarationSyntaxChecker<in D : FirDeclaration, P : PsiElement> :
    FirDeclarationChecker<D>(),
    FirSyntaxChecker<D, P> {
    override fun check(declaration: D, context: CheckerContext, reporter: DiagnosticReporter) {
        checkSyntax(declaration, context, reporter)
    }
}

abstract class FirExpressionSyntaxChecker<in E : FirStatement, P : PsiElement> :
    FirExpressionChecker<E>(),
    FirSyntaxChecker<E, P> {
    override fun check(expression: E, context: CheckerContext, reporter: DiagnosticReporter) {
        checkSyntax(expression, context, reporter)
    }
}
