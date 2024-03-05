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
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsModule
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsNonModule
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsQualifier
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker

object JsImportChecker : DeclarationChecker {
    private val annotationsForbiddenToUseWithJsImport = setOf(
        JsModule.asSingleFqName(),
        JsNonModule.asSingleFqName(),
        JsQualifier.asSingleFqName()
    )

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val trace = context.trace

        if (AnnotationsUtils.getImportSource(descriptor) == null) return

        if (descriptor is PropertyDescriptor && descriptor.isVar) {
            trace.report(ErrorsJs.JS_IMPORT_PROHIBITED_ON_VAR.on(declaration))
        }

        if (!AnnotationsUtils.isNativeObject(descriptor)) {
            trace.report(ErrorsJs.JS_IMPORT_PROHIBITED_ON_NON_NATIVE.on(declaration))
        }

        if (DescriptorUtils.isTopLevelDeclaration(descriptor) && AnnotationsUtils.getFileImportSource(trace.bindingContext, descriptor) != null) {
            trace.report(ErrorsJs.NESTED_JS_IMPORT_PROHIBITED.on(declaration))
        }

        if (
            AnnotationsUtils.getModuleName(descriptor) != null ||
            AnnotationsUtils.getContainingFileAnnotations(context.trace.bindingContext, descriptor)
                .any { it.fqName in annotationsForbiddenToUseWithJsImport }
        ) {
            trace.report(ErrorsJs.JS_IMPORT_AND_JS_MODULE_MIX.on(declaration))
        }
    }
}
