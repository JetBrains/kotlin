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

import com.google.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.serialization.KotlinSerializerExtensionBase
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol

class KotlinJavascriptSerializerExtension : KotlinSerializerExtensionBase(JsSerializerProtocol)

object JsSerializerProtocol : SerializerExtensionProtocol(
        ExtensionRegistryLite.newInstance().apply { JsProtoBuf.registerAllExtensions(this) },
        JsProtoBuf.constructorAnnotation, JsProtoBuf.classAnnotation, JsProtoBuf.functionAnnotation, JsProtoBuf.propertyAnnotation,
        JsProtoBuf.enumEntryAnnotation, JsProtoBuf.compileTimeValue, JsProtoBuf.parameterAnnotation, JsProtoBuf.typeAnnotation,
        JsProtoBuf.typeParameterAnnotation
)
