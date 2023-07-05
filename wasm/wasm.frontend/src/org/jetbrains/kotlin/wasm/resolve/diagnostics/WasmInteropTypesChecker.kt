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
import org.jetbrains.kotlin.wasm.util.hasValidJsCodeBody

// TODO: Implement in K2: KT-56849
object WasmInteropTypesChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is FunctionDescriptor) return

        if (!descriptor.annotations.hasAnnotation(FqName("kotlin.wasm.WasmExport"))) return

        val trace = context.trace
        val bindingContext = trace.bindingContext

        if (descriptor.annotations.hasAnnotation(FqName("kotlin.js.JsExport"))) {
            val reportOn = descriptor.findPsi() ?: declaration
            trace.report(ErrorsWasm.JS_AND_WASM_EXPORTS_ON_SAME_DECLARATION.on(reportOn))
        }

        if (descriptor.isEffectivelyExternal() || descriptor.hasValidJsCodeBody(bindingContext)) {
            val reportOn = descriptor.findPsi() ?: declaration
            trace.report(ErrorsWasm.WASM_EXPORT_ON_EXTERNAL_DECLARATION.on(reportOn))
        }

        if (!DescriptorUtils.isTopLevelDeclaration(descriptor)) {
            val reportOn = descriptor.findPsi() ?: declaration
            trace.report(ErrorsWasm.NESTED_WASM_EXPORT.on(reportOn))
        }
    }
}