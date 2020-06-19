/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.psi.KtElement

object LowLevelFirApiFacade {
    fun getOrBuildFirFor(element: KtElement, phase: FirResolvePhase): FirElement =
        element.getOrBuildFir(element.firResolveState(), phase)

    fun getDiagnosticsFor(element: KtElement): Collection<Diagnostic> {
        val file = element.containingKtFile
        val state = element.firResolveState()
        file.getOrBuildFirWithDiagnostics(state)
        return state.getDiagnostics(element)
    }
}