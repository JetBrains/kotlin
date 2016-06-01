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

import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.SimpleDeclarationChecker

object JsNameChecker : SimpleDeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, diagnosticHolder: DiagnosticSink,
                       bindingContext: BindingContext) {
        if (descriptor is PropertyDescriptor) {
            val namedAccessorCount = descriptor.accessors.count { AnnotationsUtils.getJsName(it) != null }
            if (namedAccessorCount > 0 && namedAccessorCount < descriptor.accessors.size) {
                diagnosticHolder.report(ErrorsJs.JS_NAME_IS_NOT_ON_ALL_ACCESSORS.on(declaration))
            }
        }

        val jsNamePsi = AnnotationsUtils.getJsNameAnnotationPsi(bindingContext, declaration) ?: return

        when (descriptor) {
            is ConstructorDescriptor -> {
                if (descriptor.isPrimary) {
                    diagnosticHolder.report(ErrorsJs.JS_NAME_ON_PRIMARY_CONSTRUCTOR_PROHIBITED.on(jsNamePsi))
                }
            }
            is PropertyAccessorDescriptor -> {
                if (!descriptor.isDefault && AnnotationsUtils.getJsName(descriptor.correspondingProperty) != null) {
                    diagnosticHolder.report(ErrorsJs.JS_NAME_ON_ACCESSOR_AND_PROPERTY.on(jsNamePsi))
                }
            }
        }
    }
}