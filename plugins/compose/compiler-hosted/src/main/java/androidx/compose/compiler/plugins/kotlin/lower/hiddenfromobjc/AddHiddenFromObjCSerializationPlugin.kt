/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin.lower.hiddenfromobjc

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.library.metadata.KlibMetadataSerializerProtocol
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.serialization.MutableVersionRequirementTable
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.serialization.DescriptorSerializerPlugin
import org.jetbrains.kotlin.serialization.SerializerExtension

/**
 * Adds the kotlin.native.HiddenFromObjC annotation to the descriptors of declarations
 * in [hideFromObjCDeclarationsSet].
 *
 * @see [HideFromObjCDeclarationsSet]
 */
class AddHiddenFromObjCSerializationPlugin(
    private val hideFromObjCDeclarationsSet: HideFromObjCDeclarationsSet
) : DescriptorSerializerPlugin {

    private val hasAnnotationFlag = Flags.HAS_ANNOTATIONS.toFlags(true)

    private val annotationToAdd = ClassId.fromString("kotlin/native/HiddenFromObjC")

    private fun createAnnotationProto(extension: SerializerExtension) =
        ProtoBuf.Annotation.newBuilder().apply {
            id = extension.stringTable.getQualifiedClassNameIndex(annotationToAdd)
        }.build()

    override fun afterClass(
        descriptor: ClassDescriptor,
        proto: ProtoBuf.Class.Builder,
        versionRequirementTable: MutableVersionRequirementTable,
        childSerializer: DescriptorSerializer,
        extension: SerializerExtension
    ) {
        if (descriptor in hideFromObjCDeclarationsSet) {
            val annotationProto = createAnnotationProto(extension)
            proto.addExtension(KlibMetadataSerializerProtocol.classAnnotation, annotationProto)
            proto.flags = proto.flags or hasAnnotationFlag
        }
    }

    override fun afterConstructor(
        descriptor: ConstructorDescriptor,
        proto: ProtoBuf.Constructor.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: DescriptorSerializer,
        extension: SerializerExtension
    ) {
        if (descriptor in hideFromObjCDeclarationsSet) {
            val annotationProto = createAnnotationProto(extension)
            proto.addExtension(
                KlibMetadataSerializerProtocol.constructorAnnotation,
                annotationProto
            )
            proto.flags = proto.flags or hasAnnotationFlag
        }
    }

    override fun afterFunction(
        descriptor: FunctionDescriptor,
        proto: ProtoBuf.Function.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: DescriptorSerializer,
        extension: SerializerExtension
    ) {
        if (descriptor in hideFromObjCDeclarationsSet) {
            val annotationProto = createAnnotationProto(extension)
            proto.addExtension(KlibMetadataSerializerProtocol.functionAnnotation, annotationProto)
            proto.flags = proto.flags or hasAnnotationFlag
        }
    }

    override fun afterProperty(
        descriptor: PropertyDescriptor,
        proto: ProtoBuf.Property.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: DescriptorSerializer,
        extension: SerializerExtension
    ) {
        if (descriptor in hideFromObjCDeclarationsSet) {
            val annotationProto = createAnnotationProto(extension)
            proto.addExtension(KlibMetadataSerializerProtocol.propertyAnnotation, annotationProto)
            proto.flags = proto.flags or hasAnnotationFlag

            // Add the annotation for the getter too if it's Composable
            val getterDescriptor = descriptor.getter
            if (getterDescriptor != null && getterDescriptor in hideFromObjCDeclarationsSet) {
                val annotationForGetter = createAnnotationProto(extension)
                proto.addExtension(
                    KlibMetadataSerializerProtocol.propertyGetterAnnotation,
                    annotationForGetter
                )
                proto.getterFlags = proto.getterFlags or hasAnnotationFlag
            }

            // Add the annotation for the setter too if it's Composable
            val setterDescriptor = descriptor.getter
            if (setterDescriptor != null && setterDescriptor in hideFromObjCDeclarationsSet) {
                val annotationForSetter = createAnnotationProto(extension)
                proto.addExtension(
                    KlibMetadataSerializerProtocol.propertySetterAnnotation,
                    annotationForSetter
                )
                proto.setterFlags = proto.setterFlags or hasAnnotationFlag
            }
        }
    }
}
