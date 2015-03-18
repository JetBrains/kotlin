/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.utils.serializer

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor

public object ClassSerializationUtil {
    public trait Sink {
        fun writeClass(classDescriptor: ClassDescriptor, classProto: ProtoBuf.Class)
    }

    private fun serializeClass(classDescriptor: ClassDescriptor, serializer: DescriptorSerializer, sink: Sink, skip: (DeclarationDescriptor) -> Boolean) {
        if (skip(classDescriptor)) return

        val classProto = serializer.classProto(classDescriptor).build() ?: error("Class not serialized: $classDescriptor")
        sink.writeClass(classDescriptor, classProto)

        serializeClasses(classDescriptor.getUnsubstitutedInnerClassesScope().getDescriptors(), serializer, sink, skip)
    }

    public fun serializeClasses(descriptors: Collection<DeclarationDescriptor>, serializer: DescriptorSerializer, sink: Sink, skip: (DeclarationDescriptor) -> Boolean) {
        for (descriptor in descriptors) {
            if (descriptor is ClassDescriptor) {
                serializeClass(descriptor, serializer, sink, skip)
            }
        }
    }
}