/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.sandbox.fir.checkers

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticRenderers.TO_STRING
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.error2
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.warning1
import org.jetbrains.kotlin.psi.KtElement

object PluginErrors : KtDiagnosticsContainer() {
    val FUNCTION_WITH_DUMMY_NAME by warning1<KtElement, String>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val ILLEGAL_NUMBER_SIGN by error2<KtElement, String, String>()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = Renderers

    object Renderers : BaseDiagnosticRendererFactory() {
        override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("Plugin sandbox errors") {
            it.put(FUNCTION_WITH_DUMMY_NAME, "{0}", TO_STRING)
            it.put(ILLEGAL_NUMBER_SIGN, "{0}, {1}", TO_STRING, TO_STRING)
        }
    }
}
