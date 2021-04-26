/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.trasformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.contracts.FirContractResolveTransformer

internal class FirDesignatedContractsResolveTransformerForIDE(
    private val firFile: FirFile,
    designation: FirDesignation,
    session: FirSession,
    scopeSession: ScopeSession,
) : FirDesignatedResolveTransformerForIDE, FirContractResolveTransformer(session, scopeSession) {
    private val ideDeclarationTransformer = IDEDeclarationTransformer(designation)

    @Suppress("NAME_SHADOWING")
    override fun transformDeclarationContent(declaration: FirDeclaration, data: ResolutionMode): FirDeclaration =
        ideDeclarationTransformer.transformDeclarationContent(this, declaration, data) { declaration, data ->
            super.transformDeclarationContent(declaration, data)
        }


    override fun needReplacePhase(firDeclaration: FirDeclaration): Boolean = ideDeclarationTransformer.needReplacePhase(firDeclaration)

    override fun transformDesignatedDeclaration() {
        firFile.transform<FirFile, ResolutionMode>(this, ResolutionMode.ContextDependent)
    }
}
