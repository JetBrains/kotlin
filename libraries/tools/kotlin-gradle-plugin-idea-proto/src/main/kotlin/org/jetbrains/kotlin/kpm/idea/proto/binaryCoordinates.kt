/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


package org.jetbrains.kotlin.kpm.idea.proto

import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmBinaryCoordinates
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmBinaryCoordinatesImpl

internal fun IdeaKpmBinaryCoordinatesProto(coordinates: IdeaKpmBinaryCoordinates): IdeaKpmBinaryCoordinatesProto {
    return ideaKpmBinaryCoordinatesProto {
        group = coordinates.group
        module = coordinates.module
        version = coordinates.version
        coordinates.kotlinModuleName?.let { kotlinModuleName = it }
        coordinates.kotlinFragmentName?.let { kotlinFragmentName = it }
    }
}

internal fun IdeaKpmBinaryCoordinates(proto: IdeaKpmBinaryCoordinatesProto): IdeaKpmBinaryCoordinates {
    return IdeaKpmBinaryCoordinatesImpl(
        group = proto.group,
        module = proto.module,
        version = proto.version,
        kotlinModuleName = if (proto.hasKotlinModuleName()) proto.kotlinModuleName else null,
        kotlinFragmentName = if (proto.hasKotlinFragmentName()) proto.kotlinFragmentName else null
    )
}

internal fun IdeaKpmBinaryCoordinates(data: ByteArray): IdeaKpmBinaryCoordinates {
    return IdeaKpmBinaryCoordinates(IdeaKpmBinaryCoordinatesProto.parseFrom(data))
}

internal fun IdeaKpmBinaryCoordinates.toByteArray(): ByteArray {
    return IdeaKpmBinaryCoordinatesProto(this).toByteArray()
}
