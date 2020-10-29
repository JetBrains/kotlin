/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirAnnotation
import org.jetbrains.kotlin.descriptors.commonizer.utils.compactConcat
import org.jetbrains.kotlin.descriptors.commonizer.utils.compactMapValues
import org.jetbrains.kotlin.resolve.constants.AnnotationValue
import org.jetbrains.kotlin.storage.getValue

class CommonizedAnnotationDescriptor(
    targetComponents: TargetDeclarationsBuilderComponents,
    cirAnnotation: CirAnnotation
) : AnnotationDescriptor {
    override val type by targetComponents.storageManager.createLazyValue {
        cirAnnotation.type.buildType(targetComponents, TypeParameterResolver.EMPTY, expandTypeAliases = true)
    }

    override val allValueArguments by targetComponents.storageManager.createLazyValue {
        cirAnnotation.constantValueArguments compactConcat cirAnnotation.annotationValueArguments.compactMapValues { (_, nestedCirAnnotation) ->
            AnnotationValue(CommonizedAnnotationDescriptor(targetComponents, nestedCirAnnotation))
        }
    }

    override val source: SourceElement get() = SourceElement.NO_SOURCE
}
