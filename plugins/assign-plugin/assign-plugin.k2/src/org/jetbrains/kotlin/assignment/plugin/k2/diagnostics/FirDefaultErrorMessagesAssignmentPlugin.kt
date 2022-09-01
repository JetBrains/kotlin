/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.assignment.plugin.k2.diagnostics

import org.jetbrains.kotlin.assignment.plugin.k2.diagnostics.FirErrorsAssignmentPlugin.CALL_ERROR_ASSIGN_METHOD_SHOULD_RETURN_UNIT
import org.jetbrains.kotlin.assignment.plugin.k2.diagnostics.FirErrorsAssignmentPlugin.DECLARATION_ERROR_ASSIGN_METHOD_SHOULD_RETURN_UNIT
import org.jetbrains.kotlin.assignment.plugin.k2.diagnostics.FirErrorsAssignmentPlugin.NO_APPLICABLE_ASSIGN_METHOD
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticRenderer

object FirDefaultErrorMessagesAssignmentPlugin {
    fun getRendererForDiagnostic(diagnostic: KtDiagnostic): KtDiagnosticRenderer {
        val factory = diagnostic.factory
        return MAP[factory] ?: factory.ktRenderer
    }

    val MAP = KtDiagnosticFactoryToRendererMap("ValueContainerAssignment").also { map ->
        map.put(
            DECLARATION_ERROR_ASSIGN_METHOD_SHOULD_RETURN_UNIT,
            "Function 'assign' used for '=' overload should return 'Unit'"
        )

        map.put(
            CALL_ERROR_ASSIGN_METHOD_SHOULD_RETURN_UNIT,
            "Function 'assign' used for '=' overload should return 'Unit'"
        )

        map.put(
            NO_APPLICABLE_ASSIGN_METHOD,
            "No applicable 'assign' function found for '=' overload"
        )
    }
}
