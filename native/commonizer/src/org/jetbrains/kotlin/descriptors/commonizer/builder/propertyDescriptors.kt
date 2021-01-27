/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirProperty
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirPropertyNode
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirNode.Companion.indexOfCommon
import org.jetbrains.kotlin.descriptors.commonizer.utils.CommonizedGroup
import org.jetbrains.kotlin.descriptors.impl.FieldDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.constants.AnnotationValue

internal fun CirPropertyNode.buildDescriptors(
    components: GlobalDeclarationsBuilderComponents,
    output: CommonizedGroup<PropertyDescriptor>,
    containingDeclarations: List<DeclarationDescriptor?>
) {
    val commonProperty = commonDeclaration()

    val isLiftedUp = commonProperty?.isLiftedUp == true
    val markAsExpectAndActual = commonProperty != null
            && commonProperty.kind != CallableMemberDescriptor.Kind.SYNTHESIZED
            && !isLiftedUp

    if (!isLiftedUp) {
        targetDeclarations.forEachIndexed { index, property ->
            property?.buildDescriptor(components, output, index, containingDeclarations, isActual = markAsExpectAndActual)
        }
    }

    commonProperty?.buildDescriptor(components, output, indexOfCommon, containingDeclarations, isExpect = markAsExpectAndActual)
}

private fun CirProperty.buildDescriptor(
    components: GlobalDeclarationsBuilderComponents,
    output: CommonizedGroup<PropertyDescriptor>,
    index: Int,
    containingDeclarations: List<DeclarationDescriptor?>,
    isExpect: Boolean = false,
    isActual: Boolean = false
) {
    val targetComponents = components.targetComponents[index]
    val containingDeclaration = containingDeclarations[index] ?: error("No containing declaration for property $this")

    val propertyDescriptor = PropertyDescriptorImpl.create(
        containingDeclaration,
        annotations.buildDescriptors(targetComponents),
        modality,
        visibility,
        isVar,
        name,
        kind,
        SourceElement.NO_SOURCE,
        isLateInit,
        isConst,
        isExpect,
        isActual,
        isExternal,
        isDelegate
    )

    val (typeParameters, typeParameterResolver) = typeParameters.buildDescriptorsAndTypeParameterResolver(
        targetComponents,
        containingDeclaration.getTypeParameterResolver(),
        propertyDescriptor
    )

    propertyDescriptor.setType(
        returnType.buildType(targetComponents, typeParameterResolver),
        typeParameters,
        buildDispatchReceiver(propertyDescriptor),
        extensionReceiver?.buildExtensionReceiver(targetComponents, typeParameterResolver, propertyDescriptor)
    )

    val getterDescriptor = getter?.let { getter ->
        DescriptorFactory.createGetter(
            propertyDescriptor,
            getter.annotations.buildDescriptors(targetComponents),
            getter.isDefault,
            getter.isExternal,
            getter.isInline
        ).apply {
            initialize(null) // use return type from the property descriptor
        }
    }

    val setterDescriptor = setter?.let { setter ->
        DescriptorFactory.createSetter(
            propertyDescriptor,
            setter.annotations.buildDescriptors(targetComponents),
            setter.parameterAnnotations.buildDescriptors(targetComponents),
            setter.isDefault,
            setter.isExternal,
            setter.isInline,
            setter.visibility,
            SourceElement.NO_SOURCE
        )
    }

    val backingField = backingFieldAnnotations?.let { annotations ->
        FieldDescriptorImpl(annotations.buildDescriptors(targetComponents), propertyDescriptor)
    }

    val delegateField = delegateFieldAnnotations?.let { annotations ->
        FieldDescriptorImpl(annotations.buildDescriptors(targetComponents), propertyDescriptor)
    }

    propertyDescriptor.initialize(
        getterDescriptor,
        setterDescriptor,
        backingField,
        delegateField
    )

    compileTimeInitializer?.let { constantValue ->
        check(constantValue !is AnnotationValue) {
            "Unexpected type of compile time constant: ${constantValue::class.java}, $constantValue"
        }
        propertyDescriptor.setCompileTimeInitializer(targetComponents.storageManager.createNullableLazyValue { constantValue })
    }

    output[index] = propertyDescriptor
}
