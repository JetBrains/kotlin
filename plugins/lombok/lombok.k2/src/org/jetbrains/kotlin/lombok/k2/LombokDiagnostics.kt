/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.errorWithoutSource
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.BaseSourcelessDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.warningWithoutSource
import org.jetbrains.kotlin.lombok.k2.LombokDiagnostics.LOMBOK_CONFIG_IS_MISSING
import org.jetbrains.kotlin.lombok.k2.LombokDiagnostics.LOMBOK_PLUGIN_IS_EXPERIMENTAL
import org.jetbrains.kotlin.lombok.k2.LombokDiagnostics.UNKNOWN_PLUGIN_OPTION
import kotlin.getValue

object LombokDiagnostics : KtDiagnosticsContainer() {
    val LOMBOK_PLUGIN_IS_EXPERIMENTAL by warningWithoutSource()
    val LOMBOK_CONFIG_IS_MISSING by warningWithoutSource()
    val UNKNOWN_PLUGIN_OPTION by errorWithoutSource()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = KtDiagnosticMessagesLombok
}

object KtDiagnosticMessagesLombok : BaseSourcelessDiagnosticRendererFactory() {
    override val MAP by KtDiagnosticFactoryToRendererMap("Lombok") { map ->
        map.put(LOMBOK_PLUGIN_IS_EXPERIMENTAL, MESSAGE_PLACEHOLDER)
        map.put(LOMBOK_CONFIG_IS_MISSING, MESSAGE_PLACEHOLDER)
        map.put(UNKNOWN_PLUGIN_OPTION, MESSAGE_PLACEHOLDER)
    }
}
