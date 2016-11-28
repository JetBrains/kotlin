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
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.js.PredefinedAnnotation
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.checkers.SimpleDeclarationChecker
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension

class JsExternalChecker : SimpleDeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, diagnosticHolder: DiagnosticSink,
                       bindingContext: BindingContext) {
        if (!AnnotationsUtils.isNativeObject(descriptor)) return

        if (!DescriptorUtils.isTopLevelDeclaration(descriptor)) {
            if (isDirectlyExternal(declaration, descriptor) && descriptor !is PropertyAccessorDescriptor) {
                diagnosticHolder.report(ErrorsJs.NESTED_EXTERNAL_DECLARATION.on(declaration))
            }
        }

        if (DescriptorUtils.isAnnotationClass(descriptor)) {
            diagnosticHolder.report(Errors.WRONG_MODIFIER_TARGET.on(declaration, KtTokens.EXTERNAL_KEYWORD, "annotation class"))
        }
        else if (descriptor is PropertyAccessorDescriptor && isDirectlyExternal(declaration, descriptor)) {
            diagnosticHolder.report(Errors.WRONG_MODIFIER_TARGET.on(declaration, KtTokens.EXTERNAL_KEYWORD, "property accessor"))
        }
        else if (isPrivateNonInlineMemberOfExternalClass(descriptor)) {
            diagnosticHolder.report(ErrorsJs.EXTERNAL_CLASS_PRIVATE_MEMBER.on(declaration))
        }

        if (descriptor !is PropertyAccessorDescriptor && descriptor.isExtension) {
            val target = when (descriptor) {
                is FunctionDescriptor -> "extension function"
                is PropertyDescriptor -> "extension property"
                else -> "extension member"
            }
            diagnosticHolder.report(Errors.WRONG_MODIFIER_TARGET.on(declaration, KtTokens.EXTERNAL_KEYWORD, target))
        }
    }

    private fun isDirectlyExternal(declaration: KtDeclaration, descriptor: DeclarationDescriptor): Boolean {
        if (declaration is KtProperty && descriptor is PropertyAccessorDescriptor) return false

        return declaration.hasModifier(KtTokens.EXTERNAL_KEYWORD) ||
               AnnotationsUtils.hasAnnotation(descriptor, PredefinedAnnotation.NATIVE)
    }

    private fun isPrivateNonInlineMemberOfExternalClass(descriptor: DeclarationDescriptor): Boolean {
        if (descriptor is PropertyAccessorDescriptor) return false
        if (descriptor !is MemberDescriptor || descriptor.visibility != Visibilities.PRIVATE) return false
        if (descriptor is FunctionDescriptor && descriptor.isInline) return false
        if (descriptor is PropertyDescriptor && descriptor.accessors.all { it.isInline }) return false

        val containingDeclaration = descriptor.containingDeclaration as? ClassDescriptor ?: return false
        return AnnotationsUtils.isNativeObject(containingDeclaration)
    }
}
