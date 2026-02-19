/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.resolve.diagnostics

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal

object WasmWasiExternalDeclarationChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is MemberDescriptor) return
        if (descriptor is PropertyAccessorDescriptor) return
        if (!DescriptorUtils.isTopLevelDeclaration(descriptor)) return
        if (!descriptor.isEffectivelyExternal()) return

        if (descriptor is FunctionDescriptor) {
            if (!descriptor.annotations.hasAnnotation(FqName("kotlin.wasm.WasmImport"))) {
                val reportOn = descriptor.findPsi() ?: declaration
                context.trace.report(ErrorsWasm.WASI_EXTERNAL_FUNCTION_WITHOUT_IMPORT.on(reportOn))
            }
        } else {
            val reportOn = descriptor.findPsi() ?: declaration
            context.trace.report(ErrorsWasm.WASI_EXTERNAL_NOT_TOP_LEVEL_FUNCTION.on(reportOn))
        }
    }
}