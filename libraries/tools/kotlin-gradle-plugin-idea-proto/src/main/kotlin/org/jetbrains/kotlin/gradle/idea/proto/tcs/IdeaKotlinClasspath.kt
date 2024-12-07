/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto.tcs

import com.google.protobuf.InvalidProtocolBufferException
import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.IdeaKotlinClasspathProto
import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.ideaKotlinClasspathProto
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinExtrasSerializer
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinSerializationContext
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinClasspath
import java.io.File

internal fun IdeaKotlinClasspath.toProto(): IdeaKotlinClasspathProto? {
    if (isEmpty()) return null
    return ideaKotlinClasspathProto {
        forEach { file -> files.add(file.absolutePath) }
    }
}

internal fun IdeaKotlinClasspathProto.toIdeaKotlinClasspath(): IdeaKotlinClasspath =
    IdeaKotlinClasspath(filesList.map(::File))


fun IdeaKotlinClasspath.toByteArray(): ByteArray = toProto()?.toByteArray() ?: byteArrayOf()

fun IdeaKotlinClasspath(data: ByteArray): IdeaKotlinClasspath? {
    return try {
        if (data.isEmpty()) return IdeaKotlinClasspath()
        IdeaKotlinClasspathProto.parseFrom(data).toIdeaKotlinClasspath()
    } catch (exception: InvalidProtocolBufferException) {
        return null
    }
}

object IdeaKotlinClasspathSerializer : IdeaKotlinExtrasSerializer<IdeaKotlinClasspath> {
    override fun serialize(context: IdeaKotlinSerializationContext, value: IdeaKotlinClasspath): ByteArray {
        return value.toByteArray()
    }

    override fun deserialize(context: IdeaKotlinSerializationContext, data: ByteArray): IdeaKotlinClasspath? {
        return try {
            if (data.isEmpty()) return IdeaKotlinClasspath()
            IdeaKotlinClasspathProto.parseFrom(data).toIdeaKotlinClasspath()
        } catch (exception: InvalidProtocolBufferException) {
            context.logger.error("Failed to deserialize ${IdeaKotlinClasspath::class.java.simpleName}", exception)
            return null
        }
    }
}
