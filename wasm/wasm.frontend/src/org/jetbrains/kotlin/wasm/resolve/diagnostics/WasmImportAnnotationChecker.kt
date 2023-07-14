/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.wasm.resolve.diagnostics

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isBoolean
import org.jetbrains.kotlin.types.typeUtil.isPrimitiveNumberType
import org.jetbrains.kotlin.types.typeUtil.isUnit

// TODO: Implement in K2: KT-56849
object WasmImportAnnotationChecker : DeclarationChecker {
    private val wasmImportFqName = FqName("kotlin.wasm.WasmImport")

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val wasmImport = descriptor.annotations.findAnnotation(wasmImportFqName) ?: return
        val trace = context.trace

        val wasmImportPsi = wasmImport.source.getPsi() ?: declaration

        if (!DescriptorUtils.isTopLevelDeclaration(descriptor)) {
            trace.report(ErrorsWasm.NESTED_WASM_IMPORT.on(wasmImportPsi))
        }

        if (descriptor is FunctionDescriptor) {
            if (!descriptor.isEffectivelyExternal()) {
                trace.report(ErrorsWasm.WASM_IMPORT_ON_NON_EXTERNAL_DECLARATION.on(wasmImportPsi))
            }
            checkSignatureIsPrimitive(descriptor, trace, declaration)
        }
    }

    private fun isParameterTypeSupported(type: KotlinType): Boolean =
        type.isPrimitiveNumberType() || type.isBoolean()

    private fun isReturnTypeSupported(type: KotlinType): Boolean =
        isParameterTypeSupported(type) || type.isUnit()

    fun checkSignatureIsPrimitive(descriptor: FunctionDescriptor, trace: BindingTrace, declaration: KtDeclaration) {
        for (parameter: ValueParameterDescriptor in descriptor.valueParameters) {
            val valueParameterDeclaration by lazy { DescriptorToSourceUtils.descriptorToDeclaration(parameter)!! }
            if (parameter.declaresDefaultValue()) {
                trace.report(ErrorsWasm.WASM_IMPORT_EXPORT_PARAMETER_DEFAULT_VALUE.on(valueParameterDeclaration))
            }
            if (parameter.isVararg) {
                trace.report(ErrorsWasm.WASM_IMPORT_EXPORT_VARARG_PARAMETER.on(valueParameterDeclaration))
            }
            if (!isParameterTypeSupported(parameter.type)) {
                trace.report(ErrorsWasm.WASM_IMPORT_EXPORT_UNSUPPORTED_PARAMETER_TYPE.on(valueParameterDeclaration, parameter.type))
            }
        }
        val returnType = descriptor.returnType
        if (returnType != null && !isReturnTypeSupported(returnType)) {
            trace.report(ErrorsWasm.WASM_IMPORT_EXPORT_UNSUPPORTED_RETURN_TYPE.on(declaration, returnType))
        }
    }
}