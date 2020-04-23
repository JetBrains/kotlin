/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.diagnostics


import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.annotations.KOTLIN_THROWS_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.descriptorUtil.firstArgument
import org.jetbrains.kotlin.utils.DFS

object NativeThrowsChecker : DeclarationChecker {
    private val throwsFqName = KOTLIN_THROWS_ANNOTATION_FQ_NAME

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val throwsAnnotation = descriptor.annotations.findAnnotation(throwsFqName)
        val reportLocation = throwsAnnotation?.let { DescriptorToSourceUtils.getSourceFromAnnotation(it) } ?: declaration

        if (!checkInheritance(declaration, descriptor, context, throwsAnnotation, reportLocation)) return

        if (throwsAnnotation != null && throwsAnnotation.getVariadicArguments().isEmpty()) {
            context.trace.report(ErrorsNative.THROWS_LIST_EMPTY.on(reportLocation))
        }
    }

    private fun checkInheritance(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext,
        throwsAnnotation: AnnotationDescriptor?,
        reportLocation: KtElement
    ): Boolean {
        if (descriptor !is CallableMemberDescriptor || descriptor.overriddenDescriptors.isEmpty()) return true

        val inherited = findInheritedThrows(descriptor).entries.distinctBy { it.value }

        if (inherited.size >= 2) {
            context.trace.report(ErrorsNative.INCOMPATIBLE_THROWS_INHERITED.on(declaration, inherited.map { it.key.containingDeclaration }))
            return false
        }

        if (throwsAnnotation == null) return true

        val (overriddenMember, overriddenThrows) = inherited.firstOrNull()
            ?: return true // Should not happen though.

        if (decodeThrowsFilter(throwsAnnotation) != overriddenThrows) {
            context.trace.report(ErrorsNative.INCOMPATIBLE_THROWS_OVERRIDE.on(reportLocation, overriddenMember.containingDeclaration))
            return false
        }

        return true
    }

    private fun findInheritedThrows(descriptor: CallableMemberDescriptor): Map<CallableMemberDescriptor, ThrowsFilter> {
        val result = mutableMapOf<CallableMemberDescriptor, ThrowsFilter>()

        DFS.dfs(
            descriptor.overriddenDescriptors,
            { current -> current.overriddenDescriptors },
            object : DFS.AbstractNodeHandler<CallableMemberDescriptor, Unit>() {
                override fun beforeChildren(current: CallableMemberDescriptor): Boolean {
                    val throwsAnnotation = current.annotations.findAnnotation(throwsFqName).takeIf { current.kind.isReal }
                    return if (throwsAnnotation == null && current.overriddenDescriptors.isNotEmpty()) {
                        // Visit overridden members:
                        true
                    } else {
                        // Take current and ignore overridden:
                        result[current.original] = decodeThrowsFilter(throwsAnnotation)
                        false
                    }
                }

                override fun result() {}
            })

        return result
    }

    private fun AnnotationDescriptor.getVariadicArguments(): List<ConstantValue<*>> {
        val argument = this.firstArgument() as? ArrayValue ?: return emptyList()
        return argument.value
    }

    private fun decodeThrowsFilter(throwsAnnotation: AnnotationDescriptor?) =
        ThrowsFilter(throwsAnnotation?.getVariadicArguments()?.toSet())

    private data class ThrowsFilter(val classes: Set<ConstantValue<*>>?)

}
