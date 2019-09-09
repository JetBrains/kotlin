/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.commonizer.CommonizedGroup
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.Property
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.PropertyNode
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.indexOfCommon
import org.jetbrains.kotlin.descriptors.impl.FieldDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.storage.StorageManager

internal fun PropertyNode.buildDescriptors(
    output: CommonizedGroup<PropertyDescriptor>,
    containingDeclarations: List<DeclarationDescriptor?>,
    storageManager: StorageManager
) {
    val commonProperty = common()

    target.forEachIndexed { index, property ->
        property?.buildDescriptor(output, index, containingDeclarations, storageManager, isActual = commonProperty != null)
    }

    commonProperty?.buildDescriptor(output, indexOfCommon, containingDeclarations, storageManager, isExpect = true)
}

private fun Property.buildDescriptor(
    output: CommonizedGroup<PropertyDescriptor>,
    index: Int,
    containingDeclarations: List<DeclarationDescriptor?>,
    storageManager: StorageManager,
    isExpect: Boolean = false,
    isActual: Boolean = false
) {
    val containingDeclaration = containingDeclarations[index] ?: error("No containing declaration for property $this")

    val propertyDescriptor = PropertyDescriptorImpl.create(
        containingDeclaration,
        annotations,
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

    propertyDescriptor.setType(
        returnType,
        typeParameters.buildDescriptors(propertyDescriptor),
        buildDispatchReceiver(propertyDescriptor),
        extensionReceiver?.buildExtensionReceiver(propertyDescriptor)
    )

    val getterDescriptor = getter?.let { getter ->
        DescriptorFactory.createGetter(
            propertyDescriptor,
            getter.annotations,
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
            setter.annotations,
            setter.parameterAnnotations,
            setter.isDefault,
            setter.isExternal,
            setter.isInline,
            setter.visibility,
            SourceElement.NO_SOURCE
        )
    }

    val backingField = backingFieldAnnotations?.let { FieldDescriptorImpl(it, propertyDescriptor) }
    val delegateField = delegateFieldAnnotations?.let { FieldDescriptorImpl(it, propertyDescriptor) }

    propertyDescriptor.initialize(
        getterDescriptor,
        setterDescriptor,
        backingField,
        delegateField
    )

    compileTimeInitializer?.let { constantValue ->
        propertyDescriptor.setCompileTimeInitializer(storageManager.createNullableLazyValue { constantValue })
    }

    output[index] = propertyDescriptor
}
