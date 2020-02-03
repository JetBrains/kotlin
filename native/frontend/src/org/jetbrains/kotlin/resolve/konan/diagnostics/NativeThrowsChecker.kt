/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.diagnostics


import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.descriptorUtil.firstArgument
import org.jetbrains.kotlin.resolve.findOriginalTopMostOverriddenDescriptors

object NativeThrowsChecker : DeclarationChecker {
    private val throwsFqName = FqName("kotlin.native.Throws")

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        checkForIncompatibleMultipleInheritance(declaration, descriptor, context)

        val throwsAnnotation = descriptor.annotations.findAnnotation(throwsFqName) ?: return
        val element = DescriptorToSourceUtils.getSourceFromAnnotation(throwsAnnotation) ?: declaration

        if (descriptor is CallableMemberDescriptor && descriptor.overriddenDescriptors.isNotEmpty()) {
            context.trace.report(ErrorsNative.THROWS_ON_OVERRIDE.on(element))
            return
        }

        if (throwsAnnotation.getVariadicArguments().isEmpty()) {
            context.trace.report(ErrorsNative.THROWS_LIST_EMPTY.on(element))
        }
    }

    private fun checkForIncompatibleMultipleInheritance(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (descriptor !is CallableMemberDescriptor) return

        if (descriptor.overriddenDescriptors.size < 2) return // No multiple inheritance here.

        val incompatible = descriptor.findOriginalTopMostOverriddenDescriptors()
            .distinctBy { it.annotations.findAnnotation(throwsFqName)?.getVariadicArguments()?.toSet() }

        if (incompatible.size < 2) return

        context.trace.report(ErrorsNative.INCOMPATIBLE_THROWS_INHERITED.on(declaration, incompatible.map { it.containingDeclaration }))
    }

    private fun AnnotationDescriptor.getVariadicArguments(): List<ConstantValue<*>> {
        val argument = this.firstArgument() as? ArrayValue ?: return emptyList()
        return argument.value
    }

}
