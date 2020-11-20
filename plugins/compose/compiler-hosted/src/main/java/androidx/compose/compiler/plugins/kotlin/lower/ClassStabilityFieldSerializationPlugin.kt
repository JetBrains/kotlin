/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.compiler.plugins.kotlin.lower

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags.HAS_ANNOTATIONS
import org.jetbrains.kotlin.metadata.serialization.MutableVersionRequirementTable
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.serialization.DescriptorSerializerPlugin
import org.jetbrains.kotlin.serialization.SerializerExtension

/**
 * A static final int is synthesized onto all classes in order to allow for stability to be
 * determined at runtime. We need to know from other modules whether or not this field got
 * synthesized or not though, and to do that, we also synthesize an annotation on the class.
 *
 * The kotlin metadata has a flag to indicate whether or not there are any annotations on the
 * class or not. If the flag is false, then a synthesized annotation will never be seen from
 * another module, so we have to use this plugin to flip the flag for all classes that we
 * synthesize the annotation on, even if the source of the class didn't have any annotations.
 */
class ClassStabilityFieldSerializationPlugin : DescriptorSerializerPlugin {
    private val hasAnnotationFlag = HAS_ANNOTATIONS.toFlags(true)
    override fun afterClass(
        descriptor: ClassDescriptor,
        proto: ProtoBuf.Class.Builder,
        versionRequirementTable: MutableVersionRequirementTable,
        childSerializer: DescriptorSerializer,
        extension: SerializerExtension
    ) {
        if (
            descriptor.visibility != DescriptorVisibilities.PUBLIC ||
            descriptor.kind == ClassKind.ENUM_CLASS ||
            descriptor.kind == ClassKind.ENUM_ENTRY ||
            descriptor.kind == ClassKind.INTERFACE ||
            descriptor.kind == ClassKind.ANNOTATION_CLASS ||
            descriptor.isExpect ||
            descriptor.isInner ||
            descriptor.isCompanionObject ||
            descriptor.isInline
        ) return

        if (proto.flags and hasAnnotationFlag == 0) {
            proto.flags = proto.flags or hasAnnotationFlag
        }
    }
}