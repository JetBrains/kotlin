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

package org.jetbrains.kotlin.js.resolve.diagnostics

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.checkers.SimpleDeclarationChecker
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDefaultValue

object JsInheritanceChecker : SimpleDeclarationChecker {
    override fun check(
            declaration: KtDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext
    ) {
        if (descriptor is FunctionDescriptor && !DescriptorUtils.isEffectivelyExternal(descriptor) &&
            isOverridingExternalWithOptionalParams(descriptor)
        ) {
            diagnosticHolder.report(ErrorsJs.OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS.on(declaration))
        }
    }

    private fun isOverridingExternalWithOptionalParams(function: FunctionDescriptor): Boolean {
        if (!function.kind.isReal) return false

        for (overriddenFunction in function.overriddenDescriptors.filter { DescriptorUtils.isEffectivelyExternal(it) }) {
            if (overriddenFunction.valueParameters.any { it.hasDefaultValue() }) return true
        }

        return false
    }
}
