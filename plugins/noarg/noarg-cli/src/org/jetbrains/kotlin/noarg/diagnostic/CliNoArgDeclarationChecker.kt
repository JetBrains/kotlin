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

package org.jetbrains.kotlin.noarg.diagnostic

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.reportFromPlugin
import org.jetbrains.kotlin.extensions.AnnotationBasedExtension
import org.jetbrains.kotlin.noarg.NO_ARG_CLASS_KEY
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDefaultValue

class CliNoArgDeclarationChecker(val noArgAnnotationFqNames: List<String>) : AbstractNoArgDeclarationChecker() {
    override fun getAnnotationFqNames(modifierListOwner: KtModifierListOwner?) = noArgAnnotationFqNames
}

abstract class AbstractNoArgDeclarationChecker : DeclarationChecker, AnnotationBasedExtension {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        // Handle only classes
        if (descriptor !is ClassDescriptor || declaration !is KtClass) return
        if (descriptor.kind != ClassKind.CLASS) return

        val hasSpecialAnnotation = descriptor.hasSpecialAnnotation(declaration)
        declaration.putUserData(NO_ARG_CLASS_KEY, hasSpecialAnnotation)
        if (hasSpecialAnnotation) {
            val superClass = descriptor.getSuperClassOrAny()
            if (superClass.constructors.none { it.isNoArgConstructor() } && !superClass.hasSpecialAnnotation(declaration)) {
                val reportTarget = declaration.nameIdentifier ?: declaration.getClassOrInterfaceKeyword() ?: declaration
                context.trace.reportFromPlugin(ErrorsNoArg.NO_NOARG_CONSTRUCTOR_IN_SUPERCLASS.on(reportTarget), DefaultErrorMessagesNoArg)
            }
        }
    }

    private fun ConstructorDescriptor.isNoArgConstructor() = (valueParameters.isEmpty()) || valueParameters.all { it.hasDefaultValue() }
}
