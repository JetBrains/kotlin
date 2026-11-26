/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.assignment.plugin.k2.diagnostics

import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.PositioningStrategyKinds.DECLARATION_RETURN_TYPE
import org.jetbrains.kotlin.diagnostics.PositioningStrategyKinds.OPERATOR
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory

object FirErrorsAssignmentPlugin : KtDiagnosticsContainer() {
    val DECLARATION_ERROR_ASSIGN_METHOD_SHOULD_RETURN_UNIT by error0(DECLARATION_RETURN_TYPE)
    val CALL_ERROR_ASSIGN_METHOD_SHOULD_RETURN_UNIT by error0(OPERATOR)
    val NO_APPLICABLE_ASSIGN_METHOD by error0(OPERATOR)

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = FirDefaultErrorMessagesAssignmentPlugin
}
