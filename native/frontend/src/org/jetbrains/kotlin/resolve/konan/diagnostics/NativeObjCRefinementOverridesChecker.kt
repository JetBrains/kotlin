/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.diagnostics

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.konan.diagnostics.NativeObjCRefinementChecker.hidesFromObjCFqName
import org.jetbrains.kotlin.resolve.konan.diagnostics.NativeObjCRefinementChecker.refinesInSwiftFqName
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope

object NativeObjCRefinementOverridesChecker : DeclarationChecker {

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is ClassDescriptor) return
        descriptor.defaultType.memberScope
            .getContributedDescriptors(DescriptorKindFilter.ALL, MemberScope.Companion.ALL_NAME_FILTER)
            .forEach {
                if (it !is CallableMemberDescriptor || it.kind.isReal) return@forEach
                check(declaration, it, context, emptyList(), emptyList())
            }
    }

    fun check(
        declarationToReport: KtDeclaration,
        descriptor: CallableMemberDescriptor,
        context: DeclarationCheckerContext,
        objCAnnotations: List<AnnotationDescriptor>,
        swiftAnnotations: List<AnnotationDescriptor>
    ) {
        if (descriptor.overriddenDescriptors.isEmpty()) return
        var isHiddenFromObjC = objCAnnotations.isNotEmpty()
        var isRefinedInSwift = swiftAnnotations.isNotEmpty()
        val supersNotHiddenFromObjC = mutableListOf<CallableMemberDescriptor>()
        val supersNotRefinedInSwift = mutableListOf<CallableMemberDescriptor>()
        for (overriddenDescriptor in descriptor.overriddenDescriptors) {
            val (superIsHiddenFromObjC, superIsRefinedInSwift) = overriddenDescriptor.inheritsRefinedAnnotations()
            if (superIsHiddenFromObjC) isHiddenFromObjC = true else supersNotHiddenFromObjC.add(overriddenDescriptor)
            if (superIsRefinedInSwift) isRefinedInSwift = true else supersNotRefinedInSwift.add(overriddenDescriptor)
        }
        if (isHiddenFromObjC && supersNotHiddenFromObjC.isNotEmpty()) {
            context.trace.reportIncompatibleOverride(declarationToReport, descriptor, objCAnnotations, supersNotHiddenFromObjC)
        }
        if (isRefinedInSwift && supersNotRefinedInSwift.isNotEmpty()) {
            context.trace.reportIncompatibleOverride(declarationToReport, descriptor, swiftAnnotations, supersNotRefinedInSwift)
        }
    }

    private fun CallableMemberDescriptor.inheritsRefinedAnnotations(): Pair<Boolean, Boolean> {
        val (hasObjC, hasSwift) = hasRefinedAnnotations()
        if (hasObjC && hasSwift) return true to true
        if (overriddenDescriptors.isEmpty()) return hasObjC to hasSwift
        // Note: `checkOverrides` requires all overridden descriptors to be either refined or not refined.
        val (inheritsObjC, inheritsSwift) = overriddenDescriptors.first().inheritsRefinedAnnotations()
        return (hasObjC || inheritsObjC) to (hasSwift || inheritsSwift)
    }

    private fun CallableMemberDescriptor.hasRefinedAnnotations(): Pair<Boolean, Boolean> {
        var hasObjC = false
        var hasSwift = false
        for (annotation in annotations) {
            val annotations = annotation.annotationClass?.annotations ?: continue
            for (metaAnnotation in annotations) {
                when (metaAnnotation.fqName) {
                    hidesFromObjCFqName -> {
                        hasObjC = true
                        break
                    }

                    refinesInSwiftFqName -> {
                        hasSwift = true
                        break
                    }
                }
            }
            if (hasObjC && hasSwift) return true to true
        }
        return hasObjC to hasSwift
    }

    private fun BindingTrace.reportIncompatibleOverride(
        declaration: KtDeclaration,
        descriptor: CallableMemberDescriptor,
        annotations: List<AnnotationDescriptor>,
        notRefinedSupers: List<CallableMemberDescriptor>
    ) {
        val containingDeclarations = notRefinedSupers.map { it.containingDeclaration }
        if (annotations.isEmpty()) {
            report(ErrorsNative.INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE.on(declaration, descriptor, containingDeclarations))
        } else {
            annotations.forEach {
                val reportLocation = DescriptorToSourceUtils.getSourceFromAnnotation(it) ?: declaration
                report(ErrorsNative.INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE.on(reportLocation, descriptor, containingDeclarations))
            }
        }
    }
}
