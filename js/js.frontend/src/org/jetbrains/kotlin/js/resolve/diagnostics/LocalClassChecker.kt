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

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.rendering.renderKind
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DeclarationChecker
import org.jetbrains.kotlin.resolve.DescriptorUtils.*

class LocalClassChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, diagnosticHolder: DiagnosticSink,
                       bindingContext: BindingContext) {
        if (descriptor !is ClassDescriptor || declaration !is KtNamedDeclaration) {
            return;
        }
        if (isAnonymousObject(descriptor) || isObject(descriptor)) {
            return
        }

        // hack to avoid to get diagnostics when compile kotlin builtins
        val fqNameUnsafe = getFqName(descriptor)
        if (fqNameUnsafe.asString().startsWith("kotlin.")) {
            return
        }

        if (hasEnclosingFunction(descriptor)) {
            diagnosticHolder.report(ErrorsJs.NON_TOPLEVEL_CLASS_DECLARATION.on(declaration, descriptor.renderKind()))
        }
    }

    private fun hasEnclosingFunction(descriptor: DeclarationDescriptor?): Boolean {
        var d = descriptor
        while (d != null) {
            if (d is FunctionDescriptor) {
                return true
            }
            d = d.containingDeclaration
        }
        return false
    }
}