/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.diagnostics

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.AnnotationChecker
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.konan.diagnostics.NativeObjCRefinementChecker.hidesFromObjCFqName
import org.jetbrains.kotlin.resolve.konan.diagnostics.NativeObjCRefinementChecker.refinesInSwiftFqName

object NativeObjCRefinementAnnotationChecker : DeclarationChecker {

    private val supportedTargets = arrayOf(KotlinTarget.FUNCTION, KotlinTarget.PROPERTY, KotlinTarget.CLASS)

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is ClassDescriptor || descriptor.kind != ClassKind.ANNOTATION_CLASS) return
        val (objCAnnotation, swiftAnnotation) = descriptor.findRefinesAnnotations()
        if (objCAnnotation == null && swiftAnnotation == null) return
        if (objCAnnotation != null && swiftAnnotation != null) {
            val reportLocation = DescriptorToSourceUtils.getSourceFromAnnotation(swiftAnnotation) ?: declaration
            context.trace.report(ErrorsNative.REDUNDANT_SWIFT_REFINEMENT.on(reportLocation))
        }
        val targets = AnnotationChecker.applicableTargetSet(descriptor)
        val unsupportedTargets = targets - supportedTargets
        if (unsupportedTargets.isNotEmpty()) {
            objCAnnotation?.let { context.trace.reportInvalidAnnotationTargets(declaration, it) }
            swiftAnnotation?.let { context.trace.reportInvalidAnnotationTargets(declaration, it) }
        }
    }

    private fun DeclarationDescriptor.findRefinesAnnotations(): Pair<AnnotationDescriptor?, AnnotationDescriptor?> {
        var objCAnnotation: AnnotationDescriptor? = null
        var swiftAnnotation: AnnotationDescriptor? = null
        for (annotation in annotations) {
            when (annotation.fqName) {
                hidesFromObjCFqName -> objCAnnotation = annotation
                refinesInSwiftFqName -> swiftAnnotation = annotation
            }
            if (objCAnnotation != null && swiftAnnotation != null) break
        }
        return objCAnnotation to swiftAnnotation
    }

    private fun BindingTrace.reportInvalidAnnotationTargets(
        declaration: KtDeclaration,
        annotation: AnnotationDescriptor
    ) {
        val reportLocation = DescriptorToSourceUtils.getSourceFromAnnotation(annotation) ?: declaration
        report(ErrorsNative.INVALID_OBJC_REFINEMENT_TARGETS.on(reportLocation))
    }
}
