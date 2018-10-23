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
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.annotations.KotlinRetention
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.getAnnotationRetention
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal
import org.jetbrains.kotlin.resolve.source.PsiSourceElement

object JsRuntimeAnnotationChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        for (annotation in descriptor.annotations) {
            val annotationClass = annotation.annotationClass ?: continue
            if (annotationClass.getAnnotationRetention() != KotlinRetention.RUNTIME) continue

            val annotationPsi = (annotation.source as? PsiSourceElement)?.psi ?: declaration

            if (descriptor is MemberDescriptor && descriptor.isEffectivelyExternal()) {
                context.trace.report(ErrorsJs.RUNTIME_ANNOTATION_ON_EXTERNAL_DECLARATION.on(annotationPsi))
            } else {
                context.trace.report(ErrorsJs.RUNTIME_ANNOTATION_NOT_SUPPORTED.on(annotationPsi))
            }
        }
    }
}
