/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.analysis.diagnostics.getAncestors
import org.jetbrains.kotlin.fir.types.FirTypeRef

fun FirTypeRef.isInTypeConstraint(): Boolean {
    val source = source ?: return false
    return source.treeStructure.getAncestors(source.lighterASTNode)
        .find { it.tokenType == KtNodeTypes.TYPE_CONSTRAINT || it.tokenType == KtNodeTypes.TYPE_PARAMETER }
        ?.tokenType == KtNodeTypes.TYPE_CONSTRAINT
}
