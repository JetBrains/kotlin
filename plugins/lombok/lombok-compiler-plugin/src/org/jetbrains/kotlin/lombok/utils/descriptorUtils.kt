/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.utils

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType

internal fun createFunction(
    containingClass: ClassDescriptor,
    name: Name,
    valueParameters: List<ValueParameterDescriptor>,
    returnType: KotlinType?,
    modality: Modality? = Modality.OPEN,
    visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC
): SimpleFunctionDescriptor {
    val methodDescriptor = SimpleFunctionDescriptorImpl.create(
        containingClass,
        Annotations.EMPTY,
        name,
        CallableMemberDescriptor.Kind.SYNTHESIZED,
        containingClass.source
    )
    methodDescriptor.initialize(
        null,
        containingClass.thisAsReceiverParameter,
        mutableListOf(),
        valueParameters,
        returnType,
        modality,
        visibility
    )
    return methodDescriptor
}
