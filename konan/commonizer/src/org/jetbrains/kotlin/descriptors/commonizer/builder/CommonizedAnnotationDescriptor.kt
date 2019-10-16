/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind.ANNOTATION_CLASS
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.AnnotationValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.storage.getValue

class CommonizedAnnotationDescriptor(
    private val targetComponents: TargetDeclarationsBuilderComponents,
    override val fqName: FqName,
    rawValueArguments: Map<Name, ConstantValue<*>>
) : AnnotationDescriptor {
    constructor(targetComponents: TargetDeclarationsBuilderComponents, cirAnnotation: CirAnnotation) : this(
        targetComponents,
        cirAnnotation.fqName,
        cirAnnotation.allValueArguments
    )

    override val type by targetComponents.storageManager.createLazyValue {
        val annotationClass = findClassOrTypeAlias(targetComponents, fqName)
        check(annotationClass is ClassDescriptor && annotationClass.kind == ANNOTATION_CLASS) {
            "Not an annotation class: ${annotationClass::class.java}, $annotationClass"
        }
        annotationClass.defaultType
    }

    override val allValueArguments by targetComponents.storageManager.createLazyValue {
        rawValueArguments.mapValues { (_, value) -> substituteValueArgument(value) }
    }

    override val source: SourceElement get() = SourceElement.NO_SOURCE

    private fun substituteValueArgument(value: ConstantValue<*>) =
        (value as? AnnotationValue)?.value?.let { nestedAnnotationDescriptor ->
            // re-build annotation descriptors
            val fqName = nestedAnnotationDescriptor.fqName
                ?: error("Annotation with no FQ name: ${nestedAnnotationDescriptor::class.java}, $nestedAnnotationDescriptor")

            AnnotationValue(CommonizedAnnotationDescriptor(targetComponents, fqName, nestedAnnotationDescriptor.allValueArguments))
        } ?: value // keep other values as they are platform agnostic
}
