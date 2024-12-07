/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto.tcs

import org.jetbrains.kotlin.gradle.idea.proto.Extras
import org.jetbrains.kotlin.gradle.idea.proto.IdeaExtrasProto
import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.IdeaKotlinUnresolvedBinaryDependencyProto
import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.ideaKotlinUnresolvedBinaryDependencyProto
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinSerializationContext
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinUnresolvedBinaryDependency
import org.jetbrains.kotlin.tooling.core.toMutableExtras

internal fun IdeaKotlinSerializationContext.IdeaKotlinUnresolvedBinaryDependencyProto(
    dependency: IdeaKotlinUnresolvedBinaryDependency
): IdeaKotlinUnresolvedBinaryDependencyProto {
    return ideaKotlinUnresolvedBinaryDependencyProto {
        this.extras = IdeaExtrasProto(dependency.extras)
        dependency.cause?.let { this.cause = it }
        dependency.coordinates?.let { this.coordinates = IdeaKotlinBinaryCoordinatesProto(it) }
    }
}

internal fun IdeaKotlinSerializationContext.IdeaKotlinUnresolvedBinaryDependency(
    proto: IdeaKotlinUnresolvedBinaryDependencyProto
): IdeaKotlinUnresolvedBinaryDependency {
    return IdeaKotlinUnresolvedBinaryDependency(
        extras = Extras(proto.extras).toMutableExtras(),
        cause = if (proto.hasCause()) proto.cause else null,
        coordinates = if (proto.hasCoordinates()) IdeaKotlinBinaryCoordinates(proto.coordinates) else null
    )
}