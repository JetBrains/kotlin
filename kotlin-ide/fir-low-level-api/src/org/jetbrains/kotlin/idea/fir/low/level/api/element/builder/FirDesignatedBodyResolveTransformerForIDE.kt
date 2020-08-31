/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.element.builder

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.createReturnTypeCalculatorForIDE
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.compose

internal class FirDesignatedBodyResolveTransformerForIDE(
    private val designation: Iterator<FirElement>,
    session: FirSession,
    scopeSession: ScopeSession,
    implicitTypeOnly: Boolean,
    private val towerDataContextCollector: FirTowerDataContextCollector? = null
) : FirBodyResolveTransformer(
    session,
    phase = FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE,
    implicitTypeOnly = implicitTypeOnly,
    scopeSession = scopeSession,
    returnTypeCalculator = createReturnTypeCalculatorForIDE(session, scopeSession)
) {

    override fun onBeforeDeclarationContentResolve(declaration: FirDeclaration) {
        towerDataContextCollector?.addDeclarationContext(declaration, context.towerDataContext)
    }

    override fun transformDeclarationContent(declaration: FirDeclaration, data: ResolutionMode): CompositeTransformResult<FirDeclaration> {
        if (designation.hasNext()) {
            designation.next().visitNoTransform(this, data)
            return declaration.compose()
        }

        return super.transformDeclarationContent(declaration, data)
    }

    override fun onBeforeStatementResolution(statement: FirStatement) {
        towerDataContextCollector?.addStatementContext(statement, context.towerDataContext)
    }
}
