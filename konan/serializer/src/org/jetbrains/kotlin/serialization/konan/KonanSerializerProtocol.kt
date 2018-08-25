/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.konan

import org.jetbrains.kotlin.metadata.konan.KonanProtoBuf
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol

object KonanSerializerProtocol : SerializerExtensionProtocol(
    ExtensionRegistryLite.newInstance().apply { KonanProtoBuf.registerAllExtensions(this) },
    KonanProtoBuf.packageFqName,
    KonanProtoBuf.constructorAnnotation,
    KonanProtoBuf.classAnnotation,
    KonanProtoBuf.functionAnnotation,
    KonanProtoBuf.propertyAnnotation,
    KonanProtoBuf.enumEntryAnnotation,
    KonanProtoBuf.compileTimeValue,
    KonanProtoBuf.parameterAnnotation,
    KonanProtoBuf.typeAnnotation,
    KonanProtoBuf.typeParameterAnnotation
)
