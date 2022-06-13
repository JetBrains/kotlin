/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.kpm.idea.proto

import com.google.protobuf.ByteString
import com.google.protobuf.InvalidProtocolBufferException
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmProject
import org.jetbrains.kotlin.gradle.kpm.idea.serialize.IdeaKpmSerializationContext
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

fun IdeaKpmSerializationContext.IdeaKpmProject(data: ByteArray): IdeaKpmProject? {
    return IdeaKpmProject(data) { IdeaKpmContainerProto.parseFrom(data) }
}

fun IdeaKpmSerializationContext.IdeaKpmProject(data: ByteBuffer): IdeaKpmProject? {
    return IdeaKpmProject(data) { IdeaKpmContainerProto.parseFrom(data) }
}

fun IdeaKpmSerializationContext.IdeaKpmProject(data: ByteString): IdeaKpmProject? {
    return IdeaKpmProject(data) { IdeaKpmContainerProto.parseFrom(data) }
}

fun IdeaKpmSerializationContext.IdeaKpmProject(stream: InputStream): IdeaKpmProject? {
    return IdeaKpmProject(stream) { IdeaKpmContainerProto.parseFrom(stream) }
}

internal fun <T> IdeaKpmSerializationContext.IdeaKpmProject(data: T, proto: (T) -> IdeaKpmContainerProto): IdeaKpmProject? {
    val container = try {
        proto(data)
    } catch (e: InvalidProtocolBufferException) {
        logger.error("Failed to deserialize IdeaKpmProject", e)
        return null
    }

    return IdeaKpmProject(container)
}

fun IdeaKpmProject.toByteArray(context: IdeaKpmSerializationContext): ByteArray {
    return context.IdeaKpmContainerProto(this).toByteArray()
}

fun IdeaKpmProject.toByteString(context: IdeaKpmSerializationContext): ByteString {
    return context.IdeaKpmContainerProto(this).toByteString()
}

fun IdeaKpmProject.writeTo(context: IdeaKpmSerializationContext, output: OutputStream) {
    context.IdeaKpmContainerProto(this).writeDelimitedTo(output)
}

internal fun IdeaKpmSerializationContext.IdeaKpmContainerProto(project: IdeaKpmProject): IdeaKpmContainerProto {
    return ideaKpmContainerProto {
        schemaVersionMajor = IdeaKpmProtoSchema.versionMajor
        schemaVersionMinor = IdeaKpmProtoSchema.versionMinor
        schemaVersionPatch = IdeaKpmProtoSchema.versionPatch
        schemaInfos.addAll(IdeaKpmProtoSchema.infos)
        this.project = IdeaKpmProjectProto(project)
    }
}

internal fun IdeaKpmSerializationContext.IdeaKpmProject(proto: IdeaKpmContainerProto): IdeaKpmProject? {
    if (!proto.hasSchemaVersionMajor()) {
        logger.error("Missing 'schema_version_major'", Throwable())
        return null
    }

    if (!proto.hasSchemaVersionMinor()) {
        logger.error("Missing 'schema_version_minor'", Throwable())
        return null
    }

    if (proto.schemaVersionMajor > IdeaKpmProtoSchema.versionMajor) {
        logger.error(
            "Incompatible IdeaKpmProto* version. Received major version ${proto.schemaVersionMajor}. " +
                    "Supported version ${IdeaKpmProtoSchema.versionMajor}", Throwable()
        )


        val relevantInfos = proto.schemaInfosList.filter { info ->
            info.sinceSchemaVersionMajor > IdeaKpmProtoSchema.versionMajor
        }

        relevantInfos.forEach { info ->
            logger.error(
                "Since: ${info.sinceSchemaVersionMajor}.${info.sinceSchemaVersionMinor}.${info.sinceSchemaVersionPatch}: ${info.message}"
            )
        }


        return null
    }

    return IdeaKpmProject(proto.project)
}
