/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.container.assignment.k2.diagnostics

import org.jetbrains.kotlin.container.assignment.k2.diagnostics.FirErrorsValueContainerAssignment.CALL_ERROR_ASSIGN_METHOD_SHOULD_RETURN_UNIT
import org.jetbrains.kotlin.container.assignment.k2.diagnostics.FirErrorsValueContainerAssignment.DECLARATION_ERROR_ASSIGN_METHOD_SHOULD_RETURN_UNIT
import org.jetbrains.kotlin.container.assignment.k2.diagnostics.FirErrorsValueContainerAssignment.NO_APPLICABLE_ASSIGN_METHOD
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticRenderer

object FirDefaultErrorMessagesValueContainerAssignment {
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
