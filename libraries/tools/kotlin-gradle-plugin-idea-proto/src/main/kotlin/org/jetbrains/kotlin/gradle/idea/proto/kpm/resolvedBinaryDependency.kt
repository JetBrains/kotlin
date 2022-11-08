/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto.kpm

import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmResolvedBinaryDependencyImpl
import org.jetbrains.kotlin.gradle.idea.proto.Extras
import org.jetbrains.kotlin.gradle.idea.proto.IdeaExtrasProto
import org.jetbrains.kotlin.gradle.idea.proto.generated.kpm.IdeaKpmResolvedBinaryDependencyProto
import org.jetbrains.kotlin.gradle.idea.proto.generated.kpm.ideaKpmResolvedBinaryDependencyProto
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaSerializationContext
import java.io.File

internal fun IdeaSerializationContext.IdeaKpmResolvedBinaryDependencyProto(
    dependency: IdeaKpmResolvedBinaryDependency
): IdeaKpmResolvedBinaryDependencyProto {
    return ideaKpmResolvedBinaryDependencyProto {
        extras = IdeaExtrasProto(dependency.extras)
        dependency.coordinates?.let { coordinates = IdeaKpmBinaryCoordinatesProto(it) }
        binaryType = dependency.binaryType
        binaryFileAbsolutePath = dependency.binaryFile.absolutePath
    }
}

internal fun IdeaSerializationContext.IdeaKpmResolvedBinaryDependency(
    proto: IdeaKpmResolvedBinaryDependencyProto
): IdeaKpmResolvedBinaryDependency {
    return IdeaKpmResolvedBinaryDependencyImpl(
        coordinates = if (proto.hasCoordinates()) IdeaKpmBinaryCoordinates(proto.coordinates) else null,
        binaryType = proto.binaryType,
        binaryFile = File(proto.binaryFileAbsolutePath),
        extras = Extras(proto.extras)
    )
}

internal fun IdeaSerializationContext.IdeaKpmResolvedBinaryDependency(data: ByteArray): IdeaKpmResolvedBinaryDependency {
    return IdeaKpmResolvedBinaryDependency(IdeaKpmResolvedBinaryDependencyProto.parseFrom(data))
}

internal fun IdeaKpmResolvedBinaryDependency.toByteArray(context: IdeaSerializationContext): ByteArray {
    return context.IdeaKpmResolvedBinaryDependencyProto(this).toByteArray()
}
