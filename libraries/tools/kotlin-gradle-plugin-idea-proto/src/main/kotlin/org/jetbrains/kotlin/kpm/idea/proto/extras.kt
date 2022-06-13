/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kpm.idea.proto

import com.google.protobuf.ByteString
import org.jetbrains.kotlin.gradle.kpm.idea.serialize.IdeaKpmSerializationContext
import org.jetbrains.kotlin.gradle.kpm.idea.serialize.IdeaKpmExtrasSerializer
import org.jetbrains.kotlin.tooling.core.Extras
import org.jetbrains.kotlin.tooling.core.toExtras
import org.jetbrains.kotlin.tooling.core.withValue

@Suppress("unchecked_cast")
internal fun IdeaKpmSerializationContext.IdeaKpmExtrasProto(extras: Extras): IdeaKpmExtrasProto {
    val context = this
    return ideaKpmExtrasProto {
        extras.entries.forEach { (key, value) ->
            val serializer = context.extrasSerializationExtension.serializer(key) ?: return@forEach
            serializer as IdeaKpmExtrasSerializer<Any>
            val serialized = runCatching { serializer.serialize(context, value) ?: return@forEach }.getOrElse { exception ->
                logger.error("Failed to serialize $key, using ${serializer.javaClass.simpleName}", exception)
                return@forEach
            }

            values.put(key.stableString, ByteString.copyFrom(serialized))
        }
    }
}

@Suppress("unchecked_cast")
internal fun IdeaKpmSerializationContext.Extras(proto: IdeaKpmExtrasProto): Extras {
    return proto.valuesMap.entries.mapNotNull { (keyString, value) ->
        val key = Extras.Key.fromString(keyString) as Extras.Key<Any>
        val serializer = extrasSerializationExtension.serializer(key) ?: return@mapNotNull null

        val deserialized = runCatching {
            serializer.deserialize(this, value.toByteArray()) ?: return@mapNotNull null
        }.getOrElse { exception ->
            logger.error("Failed to deserialize $keyString, using ${serializer.javaClass.simpleName}", exception)
            return@mapNotNull null
        }

        key withValue deserialized
    }.toExtras()
}
