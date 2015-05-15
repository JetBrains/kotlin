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

package org.jetbrains.kotlin.serialization.js

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.resolve.constants.NullValue
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.serialization.AnnotationSerializer
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.SerializerExtension
import org.jetbrains.kotlin.serialization.StringTable
import org.jetbrains.kotlin.types.JetType

public object KotlinJavascriptSerializerExtension : SerializerExtension() {
    override fun serializeClass(descriptor: ClassDescriptor, proto: ProtoBuf.Class.Builder, stringTable: StringTable) {
        for (annotation in descriptor.getAnnotations()) {
            proto.addExtension(JsProtoBuf.classAnnotation, AnnotationSerializer.serializeAnnotation(annotation, stringTable))
        }
    }

    override fun serializeCallable(
            callable: CallableMemberDescriptor,
            proto: ProtoBuf.Callable.Builder,
            stringTable: StringTable
    ) {
        for (annotation in callable.getAnnotations()) {
            proto.addExtension(JsProtoBuf.callableAnnotation, AnnotationSerializer.serializeAnnotation(annotation, stringTable))
        }
        val propertyDescriptor = callable as? PropertyDescriptor ?: return
        val compileTimeConstant = propertyDescriptor.getCompileTimeInitializer()
        if (compileTimeConstant != null && compileTimeConstant !is NullValue) {
            val type = compileTimeConstant.getType(propertyDescriptor.builtIns)
            proto.setExtension(JsProtoBuf.compileTimeValue, AnnotationSerializer.valueProto(compileTimeConstant, type, stringTable).build())
        }
    }

    override fun serializeValueParameter(
            descriptor: ValueParameterDescriptor,
            proto: ProtoBuf.Callable.ValueParameter.Builder,
            stringTable: StringTable
    ) {
        for (annotation in descriptor.getAnnotations()) {
            proto.addExtension(JsProtoBuf.parameterAnnotation, AnnotationSerializer.serializeAnnotation(annotation, stringTable))
        }
    }

    override fun serializeType(type: JetType, proto: ProtoBuf.Type.Builder, stringTable: StringTable) {
        for (annotation in type.getAnnotations()) {
            proto.addExtension(JsProtoBuf.typeAnnotation, AnnotationSerializer.serializeAnnotation(annotation, stringTable))
        }
    }
}
