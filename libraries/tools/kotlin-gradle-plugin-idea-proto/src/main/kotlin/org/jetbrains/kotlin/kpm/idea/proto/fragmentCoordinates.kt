/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kpm.idea.proto

import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmFragmentCoordinates
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmFragmentCoordinatesImpl

internal fun IdeaKpmFragmentCoordinatesProto(coordinates: IdeaKpmFragmentCoordinates): IdeaKpmFragmentCoordinatesProto {
    return ideaKpmFragmentCoordinatesProto {
        module = IdeaKpmModuleCoordinatesProto(coordinates.module)
        fragmentName = coordinates.fragmentName
    }
}

internal fun IdeaKpmFragmentCoordinates(proto: IdeaKpmFragmentCoordinatesProto): IdeaKpmFragmentCoordinates {
    return IdeaKpmFragmentCoordinatesImpl(
        module = IdeaKpmModuleCoordinates(proto.module),
        fragmentName = proto.fragmentName
    )
}

internal fun IdeaKpmFragmentCoordinates(data: ByteArray): IdeaKpmFragmentCoordinates {
    return IdeaKpmFragmentCoordinates(IdeaKpmFragmentCoordinatesProto.parseFrom(data))
}

internal fun IdeaKpmFragmentCoordinates.toByteArray(): ByteArray {
    return IdeaKpmFragmentCoordinatesProto(this).toByteArray()
}
