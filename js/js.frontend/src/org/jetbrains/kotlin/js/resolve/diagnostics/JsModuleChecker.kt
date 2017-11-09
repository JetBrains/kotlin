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
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.checkers.SimpleDeclarationChecker
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny

object JsModuleChecker : SimpleDeclarationChecker {
    override fun check(
            declaration: KtDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext
    ) {
        checkSuperClass(bindingContext, diagnosticHolder, descriptor, declaration)
        if (AnnotationsUtils.getModuleName(descriptor) == null && !AnnotationsUtils.isNonModule(descriptor)) return

        if (descriptor is PropertyDescriptor && descriptor.isVar) {
            diagnosticHolder.report(ErrorsJs.JS_MODULE_PROHIBITED_ON_VAR.on(declaration))
        }

        if (!AnnotationsUtils.isNativeObject(descriptor)) {
            diagnosticHolder.report(ErrorsJs.JS_MODULE_PROHIBITED_ON_NON_NATIVE.on(declaration))
        }

        if (DescriptorUtils.isTopLevelDeclaration(descriptor)) {
            val isFileModuleOrNonModule = AnnotationsUtils.getFileModuleName(bindingContext, descriptor) != null ||
                                          AnnotationsUtils.isFromNonModuleFile(bindingContext, descriptor)
            if (isFileModuleOrNonModule) {
                diagnosticHolder.report(ErrorsJs.NESTED_JS_MODULE_PROHIBITED.on(declaration))
            }
        }
    }

    private fun checkSuperClass(
            bindingContext: BindingContext,
            diagnosticHolder: DiagnosticSink,
            descriptor: DeclarationDescriptor,
            declaration: KtDeclaration
    ) {
        if (descriptor !is ClassDescriptor) return
        val superClass = descriptor.getSuperClassNotAny() ?: return

        val psi = (declaration as KtClassOrObject).superTypeListEntries.firstOrNull { entry ->
            bindingContext[BindingContext.TYPE, entry.typeReference]?.constructor?.declarationDescriptor == superClass
        }

        checkJsModuleUsage(bindingContext, diagnosticHolder, descriptor, superClass, psi ?: declaration)
    }
}
