/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.extensions

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.SerializationPluginMetadataExtensions
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.serialization.MutableVersionRequirementTable
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.serialization.DescriptorSerializerPlugin
import org.jetbrains.kotlin.serialization.SerializerExtension
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializableProperties
import org.jetbrains.kotlinx.serialization.compiler.resolve.findNamedCompanionAnnotation
import org.jetbrains.kotlinx.serialization.compiler.resolve.isInternalSerializable
import org.jetbrains.kotlinx.serialization.compiler.resolve.shouldHaveGeneratedMethodsInCompanion

class SerializationDescriptorSerializerPlugin : DescriptorSerializerPlugin {
    private val hasAnnotationFlag = Flags.HAS_ANNOTATIONS.toFlags(true)

    private val descriptorMetadataMap: MutableMap<ClassDescriptor, SerializableProperties> = hashMapOf()

    private val ClassDescriptor.needSaveProgramOrder: Boolean
        get() = isInternalSerializable && (modality == Modality.OPEN || modality == Modality.ABSTRACT)

    internal fun putIfNeeded(descriptor: ClassDescriptor, properties: SerializableProperties) {
        if (!descriptor.needSaveProgramOrder) return
        descriptorMetadataMap[descriptor] = properties
    }

    override fun afterClass(
        descriptor: ClassDescriptor,
        proto: ProtoBuf.Class.Builder,
        versionRequirementTable: MutableVersionRequirementTable,
        childSerializer: DescriptorSerializer,
        extension: SerializerExtension
    ) {
        fun Name.toIndex() = extension.stringTable.getStringIndex(asString())

        if (descriptor.isCompanionObject
            && descriptor.name != SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
            && (descriptor.containingDeclaration as? ClassDescriptor)?.shouldHaveGeneratedMethodsInCompanion == true
        ) {
            // named companion objects are always marked by special annotation in backend
            // in order for this annotation to be visible in runtime through reflection, it is necessary to put down the hasAnnotation flag
            if (proto.flags and hasAnnotationFlag == 0 && descriptor.findNamedCompanionAnnotation() != null) {
                proto.flags = proto.flags or hasAnnotationFlag
            }
        }

        if (!descriptor.needSaveProgramOrder) return

        val propertiesCorrectOrder = (descriptorMetadataMap[descriptor] ?: return).serializableProperties
        proto.setExtension(
            SerializationPluginMetadataExtensions.propertiesNamesInProgramOrder,
            propertiesCorrectOrder.map { it.descriptor.name.toIndex() }
        )
        descriptorMetadataMap.remove(descriptor)
    }
}
