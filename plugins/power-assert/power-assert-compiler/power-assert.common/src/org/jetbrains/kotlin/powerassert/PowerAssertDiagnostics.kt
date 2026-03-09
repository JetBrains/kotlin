/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert

import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.KtDiagnosticRenderers.TO_STRING
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression

object PowerAssertDiagnostics : KtDiagnosticsContainer() {
    override fun getRendererFactory(): BaseDiagnosticRendererFactory = PowerAssertRenderFactory

    // ===== ERRORS ===== //

    val POWER_ASSERT_ILLEGAL_EXPLANATION_ACCESS by error0<KtExpression>()

    // ===== WARNINGS ===== //

    val POWER_ASSERT_RUNTIME_UNAVAILABLE by warning0<KtElement>()
    val POWER_ASSERT_FUNCTION_NOT_TRANSFORMED by warning1<KtElement, FqName>()
    val POWER_ASSERT_CAPABLE_OVERLOAD_MISSING by warning3<KtElement, FqName, String, String>()

    // ===== INFOS ===== //

    val POWER_ASSERT_CONSTANT by DiagnosticFactory0DelegateProvider(
        Severity.INFO, SourceElementPositioningStrategies.DEFAULT, KtElement::class, this
    )
}

object PowerAssertRenderFactory : BaseDiagnosticRendererFactory() {
    override val MAP by KtDiagnosticFactoryToRendererMap("Power-Assert") { map ->
        map.put(
            factory = PowerAssertDiagnostics.POWER_ASSERT_ILLEGAL_EXPLANATION_ACCESS,
            message = "'PowerAssert.explanation' can only be accessed from within a function annotated with '@PowerAssert'.",
        )
        map.put(
            factory = PowerAssertDiagnostics.POWER_ASSERT_RUNTIME_UNAVAILABLE,
            message = "Power-Assert runtime library not available.",
        )
        map.put(
            factory = PowerAssertDiagnostics.POWER_ASSERT_FUNCTION_NOT_TRANSFORMED,
            message = "Called function ''{0}'' was not compiled with the power-assert compiler-plugin.",
            TO_STRING,
        )
        map.put(
            factory = PowerAssertDiagnostics.POWER_ASSERT_CAPABLE_OVERLOAD_MISSING,
            message = """
                Unable to find overload of function {0} for power-assert transformation callable as:
                 - {0}({1}String)
                 - {0}({1}() -> String)
                 - {0}({2}String)
                 - {0}({2}() -> String)
            """.trimIndent(),
            TO_STRING, TO_STRING, TO_STRING,
        )

        map.put(
            factory = PowerAssertDiagnostics.POWER_ASSERT_CONSTANT,
            message = "Expression is constant and will not be power-assert transformed",
        )
    }
}
