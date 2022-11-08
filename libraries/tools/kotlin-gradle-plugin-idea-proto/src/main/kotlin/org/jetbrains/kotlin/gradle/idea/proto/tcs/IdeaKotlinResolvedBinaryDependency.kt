/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto.tcs

import org.jetbrains.kotlin.gradle.idea.proto.Extras
import org.jetbrains.kotlin.gradle.idea.proto.IdeaExtrasProto
import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.IdeaKotlinResolvedBinaryDependencyProto
import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.ideaKotlinResolvedBinaryDependencyProto
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaSerializationContext
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinResolvedBinaryDependency
import org.jetbrains.kotlin.tooling.core.toMutableExtras
import java.io.File

internal fun IdeaSerializationContext.IdeaKotlinResolvedBinaryDependencyProto(
    dependency: IdeaKotlinResolvedBinaryDependency
): IdeaKotlinResolvedBinaryDependencyProto {
    return ideaKotlinResolvedBinaryDependencyProto {
        this.extras = IdeaExtrasProto(dependency.extras)
        this.binaryType = dependency.binaryType
        this.binaryFile = dependency.binaryFile.path
        dependency.coordinates?.let { this.coordinates = IdeaKotlinBinaryCoordinatesProto(it) }
    }
}

internal fun IdeaSerializationContext.IdeaKotlinResolvedBinaryDependency(
    proto: IdeaKotlinResolvedBinaryDependencyProto
): IdeaKotlinResolvedBinaryDependency {
    return IdeaKotlinResolvedBinaryDependency(
        extras = Extras(proto.extras).toMutableExtras(),
        binaryType = proto.binaryType,
        binaryFile = File(proto.binaryFile),
        coordinates = if (proto.hasCoordinates()) IdeaKotlinBinaryCoordinates(proto.coordinates) else null
    )
}