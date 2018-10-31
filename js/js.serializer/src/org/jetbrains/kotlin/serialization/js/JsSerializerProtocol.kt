/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.js

import org.jetbrains.kotlin.metadata.js.JsProtoBuf
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol

object JsSerializerProtocol : SerializerExtensionProtocol(
    ExtensionRegistryLite.newInstance().apply(JsProtoBuf::registerAllExtensions),
    JsProtoBuf.packageFqName,
    JsProtoBuf.constructorAnnotation,
    JsProtoBuf.classAnnotation,
    JsProtoBuf.functionAnnotation,
    JsProtoBuf.propertyAnnotation,
    JsProtoBuf.propertyGetterAnnotation,
    JsProtoBuf.propertySetterAnnotation,
    JsProtoBuf.enumEntryAnnotation,
    JsProtoBuf.compileTimeValue,
    JsProtoBuf.parameterAnnotation,
    JsProtoBuf.typeAnnotation,
    JsProtoBuf.typeParameterAnnotation
) {
    fun getKjsmFilePath(packageFqName: FqName): String {
        val shortName = if (packageFqName.isRoot) Name.identifier("root-package") else packageFqName.shortName()

        return packageFqName.child(shortName).asString().replace('.', '/') +
                "." +
                KotlinJavascriptSerializationUtil.CLASS_METADATA_FILE_EXTENSION
    }
}
