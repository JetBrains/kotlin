/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.errorWithoutSource
import org.jetbrains.kotlin.diagnostics.infoWithoutSource
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.BaseSourcelessDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.strongWarningWithoutSource
import org.jetbrains.kotlin.diagnostics.warningWithoutSource
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

object KaptCliDiagnostics : KtDiagnosticsContainer() {
    val KAPT_WARNING by warningWithoutSource()
    val KAPT_STRONG_WARNING by strongWarningWithoutSource()
    val KAPT_ERROR by errorWithoutSource()
    val KAPT_INFO by infoWithoutSource()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = Messages

    object Messages : BaseSourcelessDiagnosticRendererFactory() {
        override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("KaptCliDiagnostics") { map ->
            map.put(KAPT_WARNING, MESSAGE_PLACEHOLDER)
            map.put(KAPT_STRONG_WARNING, MESSAGE_PLACEHOLDER)
            map.put(KAPT_ERROR, MESSAGE_PLACEHOLDER)
            map.put(KAPT_INFO, MESSAGE_PLACEHOLDER)
        }
    }
}

class FirKaptRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        registerDiagnosticContainers(KaptCliDiagnostics)
    }
}
