/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.assignment.plugin.diagnostics

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.assignment.plugin.diagnostics.ErrorsAssignmentPlugin.DECLARATION_ERROR_ASSIGN_METHOD_SHOULD_RETURN_UNIT
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.extensions.AnnotationBasedExtension
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.types.expressions.OperatorConventions.ASSIGN_METHOD

class AssignmentPluginDeclarationChecker(private val annotations: List<String>) : DeclarationChecker {

    private val annotationMatchingService = AnnotationMatchingService(annotations)

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor is SimpleFunctionDescriptor) {
            if (!descriptor.isAssignMethod()) return
            val receiverClass = if (descriptor.isExtension) {
                descriptor.extensionReceiverParameter?.type?.constructor?.declarationDescriptor as? ClassDescriptor
            } else {
                descriptor.containingDeclaration as? ClassDescriptor
            }
            val ktFunction = declaration as? KtFunction
            if (receiverClass != null && ktFunction != null) {
                checkAssignMethod(descriptor, receiverClass, ktFunction, context.trace)
            }
        }
    }

    private fun checkAssignMethod(
        method: SimpleFunctionDescriptor,
        receiverClass: ClassDescriptor,
        declaration: KtFunction,
        diagnosticHolder: DiagnosticSink
    ) {
        if (!annotationMatchingService.isAnnotated(receiverClass)) {
            return
        }

        if (method.returnType?.let { KotlinBuiltIns.isUnit(it) } != true) {
            diagnosticHolder.report(DECLARATION_ERROR_ASSIGN_METHOD_SHOULD_RETURN_UNIT.on(declaration))
        }
    }

    private fun SimpleFunctionDescriptor.isAssignMethod(): Boolean {
        return valueParameters.size == 1 && name == ASSIGN_METHOD
    }

    private class AnnotationMatchingService(val annotations: List<String>) : AnnotationBasedExtension {
        override fun getAnnotationFqNames(modifierListOwner: KtModifierListOwner?): List<String> = annotations

        fun isAnnotated(descriptor: ClassDescriptor): Boolean = descriptor.hasSpecialAnnotation(null)
    }
}
