/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.checkers.SimpleDeclarationChecker

object JsExternalFileChecker : SimpleDeclarationChecker {
    private val annotationFqNames = setOf(AnnotationsUtils.JS_MODULE_ANNOTATION, AnnotationsUtils.JS_QUALIFIER_ANNOTATION)

    override fun check(
            declaration: KtDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext
    ) {
        if (!AnnotationsUtils.isNativeObject(descriptor) && DescriptorUtils.isTopLevelDeclaration(descriptor)) {
            AnnotationsUtils.getContainingFileAnnotations(bindingContext, descriptor).asSequence()
                    .firstOrNull { it.fqName in annotationFqNames }
                    ?.let {
                        diagnosticHolder.report(ErrorsJs.NON_EXTERNAL_DECLARATION_IN_INAPPROPRIATE_FILE.on(declaration, it.type))
                    }
        }
    }
}
