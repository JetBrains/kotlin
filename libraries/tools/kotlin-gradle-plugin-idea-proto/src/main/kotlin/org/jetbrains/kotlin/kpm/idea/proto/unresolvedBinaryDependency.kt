/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kpm.idea.proto

import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmUnresolvedBinaryDependency
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmUnresolvedBinaryDependencyImpl
import org.jetbrains.kotlin.gradle.kpm.idea.serialize.IdeaKpmSerializationContext

internal fun IdeaKpmSerializationContext.IdeaKpmUnresolvedBinaryDependencyProto(
    dependency: IdeaKpmUnresolvedBinaryDependency
): IdeaKpmUnresolvedBinaryDependencyProto {
    return ideaKpmUnresolvedBinaryDependencyProto {
        extras = IdeaKpmExtrasProto(dependency.extras)
        dependency.cause?.let { cause = it }
        dependency.coordinates?.let { coordinates = IdeaKpmBinaryCoordinatesProto(it) }
    }
}

internal fun IdeaKpmSerializationContext.IdeaKpmUnresolvedBinaryDependency(proto: IdeaKpmUnresolvedBinaryDependencyProto): IdeaKpmUnresolvedBinaryDependency {
    return IdeaKpmUnresolvedBinaryDependencyImpl(
        cause = if (proto.hasCause()) proto.cause else null,
        coordinates = if (proto.hasCoordinates()) IdeaKpmBinaryCoordinates(proto.coordinates) else null,
        extras = Extras(proto.extras)
    )
}

internal fun IdeaKpmSerializationContext.IdeaKpmUnresolvedBinaryDependency(data: ByteArray): IdeaKpmUnresolvedBinaryDependency {
    return IdeaKpmUnresolvedBinaryDependency(IdeaKpmUnresolvedBinaryDependencyProto.parseFrom(data))
}

internal fun IdeaKpmUnresolvedBinaryDependency.toByteArray(context: IdeaKpmSerializationContext): ByteArray {
    return context.IdeaKpmUnresolvedBinaryDependencyProto(this).toByteArray()
}
