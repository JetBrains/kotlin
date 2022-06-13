/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kpm.idea.proto

import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmResolvedBinaryDependencyImpl
import org.jetbrains.kotlin.gradle.kpm.idea.serialize.IdeaKpmSerializationContext
import java.io.File

internal fun IdeaKpmSerializationContext.IdeaKpmResolvedBinaryDependencyProto(
    dependency: IdeaKpmResolvedBinaryDependency
): IdeaKpmResolvedBinaryDependencyProto {
    return ideaKpmResolvedBinaryDependencyProto {
        extras = IdeaKpmExtrasProto(dependency.extras)
        dependency.coordinates?.let { coordinates = IdeaKpmBinaryCoordinatesProto(it) }
        binaryType = dependency.binaryType
        binaryFileAbsolutePath = dependency.binaryFile.absolutePath
    }
}

internal fun IdeaKpmSerializationContext.IdeaKpmResolvedBinaryDependency(
    proto: IdeaKpmResolvedBinaryDependencyProto
): IdeaKpmResolvedBinaryDependency {
    return IdeaKpmResolvedBinaryDependencyImpl(
        coordinates = if (proto.hasCoordinates()) IdeaKpmBinaryCoordinates(proto.coordinates) else null,
        binaryType = proto.binaryType,
        binaryFile = File(proto.binaryFileAbsolutePath),
        extras = Extras(proto.extras)
    )
}

internal fun IdeaKpmSerializationContext.IdeaKpmResolvedBinaryDependency(data: ByteArray): IdeaKpmResolvedBinaryDependency {
    return IdeaKpmResolvedBinaryDependency(IdeaKpmResolvedBinaryDependencyProto.parseFrom(data))
}

internal fun IdeaKpmResolvedBinaryDependency.toByteArray(context: IdeaKpmSerializationContext): ByteArray {
    return context.IdeaKpmResolvedBinaryDependencyProto(this).toByteArray()
}
