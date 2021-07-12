/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.utils

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.descriptors.JavaClassConstructorDescriptor
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaClassDescriptor
import org.jetbrains.kotlin.load.java.lazy.descriptors.isJavaField
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import org.jetbrains.kotlin.types.typeUtil.isBoolean

data class LombokValueParameter(val name: Name, val type: KotlinType)

fun ClassDescriptor.createFunction(
    name: Name,
    valueParameters: List<LombokValueParameter>,
    returnType: KotlinType?,
    typeParameters: List<TypeParameterDescriptor> = emptyList(),
    modality: Modality? = Modality.OPEN,
    visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC,
    receiver: ReceiverParameterDescriptor? = this.thisAsReceiverParameter
): SimpleFunctionDescriptor {
    val methodDescriptor = SimpleFunctionDescriptorImpl.create(
        this,
        Annotations.EMPTY,
        name,
        CallableMemberDescriptor.Kind.SYNTHESIZED,
        this.source
    )

    val paramDescriptors = valueParameters.mapIndexed { idx, param -> methodDescriptor.makeValueParameter(param, idx) }

    methodDescriptor.initialize(
        null,
        receiver,
        emptyList(),
        typeParameters,
        paramDescriptors,
        returnType,
        modality,
        visibility
    )
    return methodDescriptor
}

fun ClassDescriptor.createJavaConstructor(
    valueParameters: List<LombokValueParameter>,
    visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC
): ClassConstructorDescriptor {
    val constructor = JavaClassConstructorDescriptor.create(
        this,
        Annotations.EMPTY,
        false,
        this.source
    )
    val paramDescriptors = valueParameters.mapIndexed { idx, param -> constructor.makeValueParameter(param, idx) }
    constructor.initialize(
        null,
        constructor.calculateDispatchReceiverParameter(),
        emptyList(),
        this.declaredTypeParameters,
        paramDescriptors,
        this.defaultType,
        Modality.OPEN,
        visibility
    )
    return constructor
}

private fun CallableDescriptor.makeValueParameter(param: LombokValueParameter, index: Int): ValueParameterDescriptor {
    return ValueParameterDescriptorImpl(
        containingDeclaration = this,
        original = null,
        index = index,
        annotations = Annotations.EMPTY,
        name = param.name,
        outType = param.type,
        declaresDefaultValue = false,
        isCrossinline = false,
        isNoinline = false,
        varargElementType = null,
        source = this.source
    )
}

fun ClassDescriptor.getJavaFields(): List<PropertyDescriptor> {
    val variableNames = getJavaClass()?.fields?.map { it.name } ?: emptyList()
    return variableNames
        .mapNotNull { this.unsubstitutedMemberScope.getContributedVariables(it, NoLookupLocation.FROM_SYNTHETIC_SCOPE).singleOrNull() }
        .filter { it.isJavaField }
}

fun KotlinType.isPrimitiveBoolean(): Boolean = this is SimpleTypeMarker && isBoolean()

//we process local java files only
fun ClassDescriptor.getJavaClass(): JavaClassImpl? =
    (this as? LazyJavaClassDescriptor)?.jClass as? JavaClassImpl
