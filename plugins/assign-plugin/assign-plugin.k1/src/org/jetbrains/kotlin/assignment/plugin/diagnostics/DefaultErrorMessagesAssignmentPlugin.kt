/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.assignment.plugin.diagnostics

import org.jetbrains.kotlin.assignment.plugin.diagnostics.ErrorsAssignmentPlugin.*
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap

object DefaultErrorMessagesAssignmentPlugin : DefaultErrorMessages.Extension {

    private val MAP = DiagnosticFactoryToRendererMap("ValueContainerAssignment")

    override fun getMap() = MAP

    init {
        MAP.put(
            DECLARATION_ERROR_ASSIGN_METHOD_SHOULD_RETURN_UNIT,
            "Function 'assign' used for '=' overload should return 'Unit'"
        )

        MAP.put(
            CALL_ERROR_ASSIGN_METHOD_SHOULD_RETURN_UNIT,
            "Function 'assign' used for '=' overload should return 'Unit'"
        )

        MAP.put(
            NO_APPLICABLE_ASSIGN_METHOD,
            "No applicable 'assign' function found for '=' overload"
        )
    }
}
