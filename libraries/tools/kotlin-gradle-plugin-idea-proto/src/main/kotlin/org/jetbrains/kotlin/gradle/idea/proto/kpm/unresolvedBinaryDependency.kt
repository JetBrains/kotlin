/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto.kpm

import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmUnresolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmUnresolvedBinaryDependencyImpl
import org.jetbrains.kotlin.gradle.idea.proto.Extras
import org.jetbrains.kotlin.gradle.idea.proto.IdeaExtrasProto
import org.jetbrains.kotlin.gradle.idea.proto.generated.kpm.IdeaKpmUnresolvedBinaryDependencyProto
import org.jetbrains.kotlin.gradle.idea.proto.generated.kpm.ideaKpmUnresolvedBinaryDependencyProto
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaSerializationContext

internal fun IdeaSerializationContext.IdeaKpmUnresolvedBinaryDependencyProto(
    dependency: IdeaKpmUnresolvedBinaryDependency
): IdeaKpmUnresolvedBinaryDependencyProto {
    return ideaKpmUnresolvedBinaryDependencyProto {
        extras = IdeaExtrasProto(dependency.extras)
        dependency.cause?.let { cause = it }
        dependency.coordinates?.let { coordinates = IdeaKpmBinaryCoordinatesProto(it) }
    }
}

internal fun IdeaSerializationContext.IdeaKpmUnresolvedBinaryDependency(proto: IdeaKpmUnresolvedBinaryDependencyProto): IdeaKpmUnresolvedBinaryDependency {
    return IdeaKpmUnresolvedBinaryDependencyImpl(
        cause = if (proto.hasCause()) proto.cause else null,
        coordinates = if (proto.hasCoordinates()) IdeaKpmBinaryCoordinates(proto.coordinates) else null,
        extras = Extras(proto.extras)
    )
}

internal fun IdeaSerializationContext.IdeaKpmUnresolvedBinaryDependency(data: ByteArray): IdeaKpmUnresolvedBinaryDependency {
    return IdeaKpmUnresolvedBinaryDependency(IdeaKpmUnresolvedBinaryDependencyProto.parseFrom(data))
}

internal fun IdeaKpmUnresolvedBinaryDependency.toByteArray(context: IdeaSerializationContext): ByteArray {
    return context.IdeaKpmUnresolvedBinaryDependencyProto(this).toByteArray()
}
