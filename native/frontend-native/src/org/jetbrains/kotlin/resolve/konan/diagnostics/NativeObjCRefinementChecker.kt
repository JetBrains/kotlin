/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.diagnostics

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.konan.diagnostics.NativeObjCRefinementOverridesChecker.check

object NativeObjCRefinementChecker : DeclarationChecker {

    val hidesFromObjCFqName = FqName("kotlin.native.HidesFromObjC")
    val refinesInSwiftFqName = FqName("kotlin.native.RefinesInSwift")

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is CallableMemberDescriptor) return
        if (descriptor !is FunctionDescriptor && descriptor !is PropertyDescriptor) return
        val (objCAnnotations, swiftAnnotations) = descriptor.findRefinedAnnotations()
        if (objCAnnotations.isNotEmpty() && swiftAnnotations.isNotEmpty()) {
            swiftAnnotations.forEach {
                val reportLocation = DescriptorToSourceUtils.getSourceFromAnnotation(it) ?: declaration
                context.trace.report(ErrorsNative.REDUNDANT_SWIFT_REFINEMENT.on(reportLocation))
            }
        }
        check(declaration, descriptor, context, objCAnnotations, swiftAnnotations)
    }

    private fun DeclarationDescriptor.findRefinedAnnotations(): Pair<List<AnnotationDescriptor>, List<AnnotationDescriptor>> {
        val objCAnnotations = mutableListOf<AnnotationDescriptor>()
        val swiftAnnotations = mutableListOf<AnnotationDescriptor>()
        for (annotation in annotations) {
            val annotations = annotation.annotationClass?.annotations ?: continue
            for (metaAnnotation in annotations) {
                when (metaAnnotation.fqName) {
                    hidesFromObjCFqName -> {
                        objCAnnotations.add(annotation)
                        break
                    }

                    refinesInSwiftFqName -> {
                        swiftAnnotations.add(annotation)
                        break
                    }
                }
            }
        }
        return objCAnnotations to swiftAnnotations
    }
}
