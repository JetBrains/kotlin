/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind.ANNOTATION_CLASS
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirAnnotation
import org.jetbrains.kotlin.descriptors.commonizer.utils.concat
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.constants.AnnotationValue
import org.jetbrains.kotlin.storage.getValue

class CommonizedAnnotationDescriptor(
    targetComponents: TargetDeclarationsBuilderComponents,
    cirAnnotation: CirAnnotation
) : AnnotationDescriptor {
    override val fqName: FqName = cirAnnotation.fqName

    override val type by targetComponents.storageManager.createLazyValue {
        val annotationClass = findClassOrTypeAlias(targetComponents, fqName)
        check(annotationClass is ClassDescriptor && annotationClass.kind == ANNOTATION_CLASS) {
            "Not an annotation class: ${annotationClass::class.java}, $annotationClass"
        }
        annotationClass.defaultType
    }

    override val allValueArguments by targetComponents.storageManager.createLazyValue {
        cirAnnotation.constantValueArguments concat cirAnnotation.annotationValueArguments.mapValues { (_, nestedCirAnnotation) ->
            AnnotationValue(CommonizedAnnotationDescriptor(targetComponents, nestedCirAnnotation))
        }
    }

    override val source: SourceElement get() = SourceElement.NO_SOURCE
}
