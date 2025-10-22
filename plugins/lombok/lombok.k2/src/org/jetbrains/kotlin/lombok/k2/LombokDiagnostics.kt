/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.BaseSourcelessDiagnosticFactory
import org.jetbrains.kotlin.diagnostics.warningWithoutSource
import org.jetbrains.kotlin.lombok.k2.LombokDiagnostics.LOMBOK_PLUGIN_IS_EXPERIMENTAL
import kotlin.getValue

object LombokDiagnostics : KtDiagnosticsContainer() {
    val LOMBOK_PLUGIN_IS_EXPERIMENTAL by warningWithoutSource()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = KtDiagnosticMessagesLombok
}

object KtDiagnosticMessagesLombok : BaseSourcelessDiagnosticFactory() {
    override val MAP by KtDiagnosticFactoryToRendererMap("Lombok") { map ->
        map.put(LOMBOK_PLUGIN_IS_EXPERIMENTAL, MESSAGE_PLACEHOLDER)
    }
}