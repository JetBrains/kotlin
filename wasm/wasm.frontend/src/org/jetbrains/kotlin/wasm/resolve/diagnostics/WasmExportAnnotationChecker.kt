/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.resolve.diagnostics

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.wasm.util.hasValidJsCodeBody

abstract class WasmExportAnnotationChecker(val checkJsInterop: Boolean) : DeclarationChecker {
    private val wasmExportFqName = FqName("kotlin.wasm.WasmExport")
    private val jsExportFqName = FqName("kotlin.js.JsExport")

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is FunctionDescriptor) return
        val wasmExport = descriptor.annotations.findAnnotation(wasmExportFqName) ?: return

        val wasmExportPsi = wasmExport.source.getPsi() ?: declaration

        val trace = context.trace
        val bindingContext = trace.bindingContext

        if (checkJsInterop) {
            if (descriptor.annotations.hasAnnotation(jsExportFqName)) {
                val reportOn = descriptor.findPsi() ?: declaration
                trace.report(ErrorsWasm.JS_AND_WASM_EXPORTS_ON_SAME_DECLARATION.on(reportOn))
            }
        }

        if (descriptor.isEffectivelyExternal() || (checkJsInterop && descriptor.hasValidJsCodeBody(bindingContext))) {
            trace.report(ErrorsWasm.WASM_EXPORT_ON_EXTERNAL_DECLARATION.on(wasmExportPsi))
        }

        if (!DescriptorUtils.isTopLevelDeclaration(descriptor)) {
            trace.report(ErrorsWasm.NESTED_WASM_EXPORT.on(wasmExportPsi))
        }

        WasmImportAnnotationChecker.checkSignatureIsPrimitive(descriptor, trace, declaration)
    }
}

object WasmJsExportChecker : WasmExportAnnotationChecker(checkJsInterop = true)

object WasmWasiExportChecker : WasmExportAnnotationChecker(checkJsInterop = false)