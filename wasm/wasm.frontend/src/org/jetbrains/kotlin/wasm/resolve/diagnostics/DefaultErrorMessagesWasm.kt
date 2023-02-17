/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.resolve.diagnostics

import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.Renderers

private val DIAGNOSTIC_FACTORY_TO_RENDERER by lazy {
    DiagnosticFactoryToRendererMap("Wasm").apply {
        put(
            ErrorsWasm.NON_EXTERNAL_TYPE_EXTENDS_EXTERNAL_TYPE,
            "Non-external type extends external type {0}",
            Renderers.RENDER_TYPE
        )

        put(ErrorsWasm.NESTED_WASM_IMPORT, "Only top-level functions can be imported with @WasmImport")
        put(ErrorsWasm.WASM_IMPORT_ON_NON_EXTERNAL_DECLARATION, "Functions annotated with @WasmImport must be external")
    }
}

class DefaultErrorMessagesWasm : DefaultErrorMessages.Extension {
    override fun getMap(): DiagnosticFactoryToRendererMap = DIAGNOSTIC_FACTORY_TO_RENDERER
}