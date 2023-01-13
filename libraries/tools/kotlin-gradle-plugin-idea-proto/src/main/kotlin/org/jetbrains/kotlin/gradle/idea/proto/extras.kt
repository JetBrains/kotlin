/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.idea.proto

import com.google.protobuf.ByteString
import com.google.protobuf.InvalidProtocolBufferException
import org.jetbrains.kotlin.gradle.idea.proto.generated.IdeaExtrasProto
import org.jetbrains.kotlin.gradle.idea.proto.generated.ideaExtrasProto
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinExtrasSerializer
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinSerializationContext
import org.jetbrains.kotlin.tooling.core.*

fun Extras.toByteArray(context: IdeaKotlinSerializationContext): ByteArray {
    return context.IdeaExtrasProto(this).toByteArray()
}

fun IdeaKotlinSerializationContext.Extras(data: ByteArray): MutableExtras? {
    return try {
        val proto = IdeaExtrasProto.parseFrom(data)
        Extras(proto)

    } catch (e: InvalidProtocolBufferException) {
        logger.error("Failed to deserialize Extras", e)
        null
    }
}

@Suppress("unchecked_cast")
internal fun IdeaKotlinSerializationContext.IdeaExtrasProto(extras: Extras): IdeaExtrasProto {
    val context = this
    return ideaExtrasProto {
        extras.entries.forEach { (key, value) ->
            val serializer = context.extrasSerializationExtension.serializer(key) ?: return@forEach
            serializer as IdeaKotlinExtrasSerializer<Any>
            val serialized = runCatching { serializer.serialize(context, value) ?: return@forEach }.getOrElse { exception ->
                logger.error("Failed to serialize $key, using ${serializer.javaClass.simpleName}", exception)
                return@forEach
            }

            values.put(key.stableString, ByteString.copyFrom(serialized))
        }
    }
}

@Suppress("unchecked_cast")
internal fun IdeaKotlinSerializationContext.Extras(proto: IdeaExtrasProto): MutableExtras {
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
    }.toMutableExtras()
}
