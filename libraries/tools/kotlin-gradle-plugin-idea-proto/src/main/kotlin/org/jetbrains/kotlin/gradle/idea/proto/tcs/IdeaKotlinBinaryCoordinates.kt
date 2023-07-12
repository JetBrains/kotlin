/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto.tcs

import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.IdeaKotlinBinaryCoordinatesProto
import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.ideaKotlinBinaryCoordinatesProto
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryCoordinates

internal fun IdeaKotlinBinaryCoordinatesProto(
    coordinates: IdeaKotlinBinaryCoordinates,
): IdeaKotlinBinaryCoordinatesProto {
    return ideaKotlinBinaryCoordinatesProto {
        this.group = coordinates.group
        this.module = coordinates.module
        coordinates.version?.let { this.version = it }
        coordinates.sourceSetName?.let { this.sourceSetName = it }
        coordinates.capabilities.forEach { capability ->
            this.capabilities.add(IdeaKotlinBinaryCapabilityProto(capability))
        }
        this.attributes = IdeaKotlinBinaryAttributesProto(coordinates.attributes)
    }
}

internal fun IdeaKotlinBinaryCoordinates(proto: IdeaKotlinBinaryCoordinatesProto): IdeaKotlinBinaryCoordinates {
    return IdeaKotlinBinaryCoordinates(
        group = proto.group,
        module = proto.module,
        version = if (proto.hasVersion()) proto.version else null,
        sourceSetName = if (proto.hasSourceSetName()) proto.sourceSetName else null,
        capabilities = proto.capabilitiesList.map(::IdeaKotlinBinaryCapability).toSet(),
        attributes = IdeaKotlinBinaryAttributes(proto.attributes)
    )
}
