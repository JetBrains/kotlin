/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.analyze

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.JsStandardClassIds
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.checkers.PlatformDiagnosticSuppressor
import org.jetbrains.kotlin.wasm.util.hasValidJsCodeBody


object WasmDiagnosticSuppressor : PlatformDiagnosticSuppressor {
    override fun shouldReportUnusedParameter(parameter: VariableDescriptor, bindingContext: BindingContext): Boolean {
        val containingDeclaration = parameter.containingDeclaration
        if (containingDeclaration is FunctionDescriptor) {
            return !containingDeclaration.hasValidJsCodeBody(bindingContext)
        }
        return true
    }

    override fun shouldReportNoBody(descriptor: CallableMemberDescriptor): Boolean = true
}