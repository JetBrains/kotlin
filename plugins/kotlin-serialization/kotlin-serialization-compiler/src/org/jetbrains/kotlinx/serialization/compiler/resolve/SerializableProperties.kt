/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.resolve

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.hasBackingField
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.serialization.deserialization.getName
import org.jetbrains.kotlinx.serialization.compiler.diagnostic.SERIALIZABLE_PROPERTIES
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationComponentRegistrar
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationDescriptorSerializerPlugin
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationPluginMetadataExtensions

class SerializableProperties(private val serializableClass: ClassDescriptor, val bindingContext: BindingContext) {
    private val primaryConstructorParameters: List<ValueParameterDescriptor> =
        serializableClass.unsubstitutedPrimaryConstructor?.valueParameters ?: emptyList()

    val serializableProperties: List<SerializableProperty>
    val isExternallySerializable: Boolean
    private val primaryConstructorProperties: Map<PropertyDescriptor, Boolean>


    init {
        val descriptorsSequence = serializableClass.unsubstitutedMemberScope.getContributedDescriptors(DescriptorKindFilter.VARIABLES)
            .asSequence()
        // call to any BindingContext.get should be only AFTER MemberScope.getContributedDescriptors
        primaryConstructorProperties =
            primaryConstructorParameters.asSequence()
                .map { parameter -> bindingContext[BindingContext.VALUE_PARAMETER_AS_PROPERTY, parameter] to parameter.declaresDefaultValue() }
                .mapNotNull { (a, b) -> if (a == null) null else a to b }
                .toMap()

        fun isPropSerializable(it: PropertyDescriptor) =
            if (serializableClass.isInternalSerializable) !it.annotations.serialTransient
            else !DescriptorVisibilities.isPrivate(it.visibility) && ((it.isVar && !it.annotations.serialTransient) || primaryConstructorProperties.contains(
                it
            ))

        serializableProperties = descriptorsSequence.filterIsInstance<PropertyDescriptor>()
            .filter { it.kind == CallableMemberDescriptor.Kind.DECLARATION }
            .filter(::isPropSerializable)
            .map { prop ->
                SerializableProperty(
                    prop,
                    primaryConstructorProperties[prop] ?: false,
                    prop.hasBackingField(bindingContext) || (prop is DeserializedPropertyDescriptor && prop.backingField != null) // workaround for TODO in .hasBackingField
                )
            }
            .filterNot { it.transient }
            .partition { primaryConstructorProperties.contains(it.descriptor) }
            .run {
                val supers = serializableClass.getSuperClassNotAny()
                if (supers == null || !supers.isInternalSerializable)
                    first + second
                else
                    SerializableProperties(supers, bindingContext).serializableProperties + first + second
            }
            .let { unsort(serializableClass, it) }

        isExternallySerializable =
            serializableClass.isSerializableEnum() || primaryConstructorParameters.size == primaryConstructorProperties.size

    }

    val serializableConstructorProperties: List<SerializableProperty> =
        serializableProperties.asSequence()
            .filter { primaryConstructorProperties.contains(it.descriptor) }
            .toList()

    val serializableStandaloneProperties: List<SerializableProperty> =
        serializableProperties.minus(serializableConstructorProperties)

    val size = serializableProperties.size
    operator fun get(index: Int) = serializableProperties[index]
    operator fun iterator() = serializableProperties.iterator()

    val primaryConstructorWithDefaults = serializableClass.unsubstitutedPrimaryConstructor
        ?.original?.valueParameters?.any { it.declaresDefaultValue() } ?: false
}

internal fun List<SerializableProperty>.bitMaskSlotCount() = size / 32 + 1
internal fun bitMaskSlotAt(propertyIndex: Int) = propertyIndex / 32

internal fun BindingContext.serializablePropertiesFor(classDescriptor: ClassDescriptor, serializationDescriptorSerializer: SerializationDescriptorSerializerPlugin? = null): SerializableProperties {
    val props = this.get(SERIALIZABLE_PROPERTIES, classDescriptor) ?: SerializableProperties(classDescriptor, this)
    serializationDescriptorSerializer?.putIfNeeded(classDescriptor, props)
    return props
}

private fun unsort(descriptor: ClassDescriptor, props: List<SerializableProperty>): List<SerializableProperty> {
    if (descriptor !is DeserializedClassDescriptor) return props
    val correctOrder: List<Name> = descriptor.classProto.getExtension(SerializationPluginMetadataExtensions.propertiesNamesInProgramOrder)
        .map { descriptor.c.nameResolver.getName(it) }
    val propsMap = props.associateBy { it.descriptor.name }
    return correctOrder.map { propsMap.getValue(it) }
}
