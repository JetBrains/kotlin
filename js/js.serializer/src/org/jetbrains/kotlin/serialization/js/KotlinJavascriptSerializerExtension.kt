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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.constants.NullValue
import org.jetbrains.kotlin.serialization.AnnotationSerializer
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.SerializerExtension
import org.jetbrains.kotlin.serialization.StringTableImpl
import org.jetbrains.kotlin.types.JetType

public class KotlinJavascriptSerializerExtension : SerializerExtension() {
    private val stringTable = StringTableImpl()
    private val annotationSerializer = AnnotationSerializer(stringTable)

    override fun getStringTable() = stringTable

    override fun serializeClass(descriptor: ClassDescriptor, proto: ProtoBuf.Class.Builder) {
        for (annotation in descriptor.annotations) {
            proto.addExtension(JsProtoBuf.classAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
    }

    override fun serializeConstructor(descriptor: ConstructorDescriptor, proto: ProtoBuf.Constructor.Builder) {
        for (annotation in descriptor.annotations) {
            proto.addExtension(JsProtoBuf.constructorAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
    }

    override fun serializeFunction(descriptor: FunctionDescriptor, proto: ProtoBuf.Function.Builder) {
        for (annotation in descriptor.annotations) {
            proto.addExtension(JsProtoBuf.functionAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
    }

    override fun serializeProperty(descriptor: PropertyDescriptor, proto: ProtoBuf.Property.Builder) {
        for (annotation in descriptor.annotations) {
            proto.addExtension(JsProtoBuf.propertyAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
        val constantInitializer = descriptor.compileTimeInitializer ?: return
        if (constantInitializer !is NullValue) {
            proto.setExtension(JsProtoBuf.compileTimeValue, annotationSerializer.valueProto(constantInitializer).build())
        }
    }

    override fun serializeValueParameter(descriptor: ValueParameterDescriptor, proto: ProtoBuf.ValueParameter.Builder) {
        for (annotation in descriptor.annotations) {
            proto.addExtension(JsProtoBuf.parameterAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
    }

    override fun serializeType(type: JetType, proto: ProtoBuf.Type.Builder) {
        for (annotation in type.annotations) {
            proto.addExtension(JsProtoBuf.typeAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
    }
}
