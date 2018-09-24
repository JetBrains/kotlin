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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.PredefinedAnnotation
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.source.getPsi

object JsNameChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val trace = context.trace
        if (descriptor is PropertyDescriptor) {
            val namedAccessorCount = descriptor.accessors.count { AnnotationsUtils.getJsName(it) != null }
            if (namedAccessorCount > 0 && namedAccessorCount < descriptor.accessors.size) {
                trace.report(ErrorsJs.JS_NAME_IS_NOT_ON_ALL_ACCESSORS.on(declaration))
            }
        }

        val jsName = AnnotationsUtils.getJsNameAnnotation(descriptor) ?: return
        val jsNamePsi = jsName.source.getPsi() ?: declaration

        if (AnnotationsUtils.getNameForAnnotatedObject(descriptor, PredefinedAnnotation.NATIVE) != null) {
            trace.report(ErrorsJs.JS_NAME_PROHIBITED_FOR_NAMED_NATIVE.on(jsNamePsi))
        }

        if (descriptor is CallableMemberDescriptor && descriptor.overriddenDescriptors.isNotEmpty()) {
            trace.report(ErrorsJs.JS_NAME_PROHIBITED_FOR_OVERRIDE.on(jsNamePsi))
        }

        when (descriptor) {
            is ConstructorDescriptor -> {
                if (descriptor.isPrimary) {
                    trace.report(ErrorsJs.JS_NAME_ON_PRIMARY_CONSTRUCTOR_PROHIBITED.on(jsNamePsi))
                }
            }
            is PropertyAccessorDescriptor -> {
                if (AnnotationsUtils.getJsName(descriptor.correspondingProperty) != null) {
                    trace.report(ErrorsJs.JS_NAME_ON_ACCESSOR_AND_PROPERTY.on(jsNamePsi))
                }
            }
            is PropertyDescriptor -> {
                if (descriptor.isExtension) {
                    trace.report(ErrorsJs.JS_NAME_PROHIBITED_FOR_EXTENSION_PROPERTY.on(jsNamePsi))
                }
            }
        }
    }
}
