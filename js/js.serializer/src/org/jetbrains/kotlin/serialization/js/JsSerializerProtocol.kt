/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.js

import org.jetbrains.kotlin.metadata.ExtensionRegistryLite
import org.jetbrains.kotlin.metadata.SerializationPluginMetadataExtensions
import org.jetbrains.kotlin.metadata.js.JsProtoBuf
import org.jetbrains.kotlin.metadata.js.JsProtoBuf.registerAllExtensions
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol

object JsSerializerProtocol : SerializerExtensionProtocol(
    ExtensionRegistryLite {
        registerAllExtensions(this)
        SerializationPluginMetadataExtensions.registerAllExtensions(this)
    },
    JsProtoBuf.packageFqName,
    JsProtoBuf.constructorAnnotation,
    JsProtoBuf.classAnnotation,
    JsProtoBuf.functionAnnotation,
    functionExtensionReceiverAnnotation = null,
    JsProtoBuf.propertyAnnotation,
    JsProtoBuf.propertyGetterAnnotation,
    JsProtoBuf.propertySetterAnnotation,
    propertyExtensionReceiverAnnotation = null,
    propertyBackingFieldAnnotation = null,
    propertyDelegatedFieldAnnotation = null,
    JsProtoBuf.enumEntryAnnotation,
    JsProtoBuf.compileTimeValue,
    JsProtoBuf.parameterAnnotation,
    JsProtoBuf.typeAnnotation,
    JsProtoBuf.typeParameterAnnotation
)
