/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kpm.idea.proto

import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmFragmentDependency
import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmFragmentDependencyImpl
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaSerializationContext

internal fun IdeaSerializationContext.IdeaKpmFragmentDependencyProto(dependency: IdeaKpmFragmentDependency): IdeaKpmFragmentDependencyProto {
    return ideaKpmFragmentDependencyProto {
        type = when (dependency.type) {
            IdeaKpmFragmentDependency.Type.Regular -> IdeaKpmFragmentDependencyProto.Type.REGULAR
            IdeaKpmFragmentDependency.Type.Friend -> IdeaKpmFragmentDependencyProto.Type.FRIEND
            IdeaKpmFragmentDependency.Type.Refines -> IdeaKpmFragmentDependencyProto.Type.REFINES
        }

        coordinates = IdeaKpmFragmentCoordinatesProto(dependency.coordinates)
        extras = IdeaKpmExtrasProto(dependency.extras)
    }
}

internal fun IdeaSerializationContext.IdeaKpmFragmentDependency(proto: IdeaKpmFragmentDependencyProto): IdeaKpmFragmentDependency {
    return IdeaKpmFragmentDependencyImpl(
        type = when (proto.type) {
            IdeaKpmFragmentDependencyProto.Type.REGULAR -> IdeaKpmFragmentDependency.Type.Regular
            IdeaKpmFragmentDependencyProto.Type.FRIEND -> IdeaKpmFragmentDependency.Type.Friend
            IdeaKpmFragmentDependencyProto.Type.REFINES -> IdeaKpmFragmentDependency.Type.Refines
            else -> IdeaKpmFragmentDependency.Type.Regular
        },
        coordinates = IdeaKpmFragmentCoordinates(proto.coordinates),
        extras = Extras(proto.extras)
    )
}

internal fun IdeaSerializationContext.IdeaKpmFragmentDependency(data: ByteArray): IdeaKpmFragmentDependency {
    return IdeaKpmFragmentDependency(IdeaKpmFragmentDependencyProto.parseFrom(data))
}

internal fun IdeaKpmFragmentDependency.toByteArray(context: IdeaSerializationContext): ByteArray {
    return context.IdeaKpmFragmentDependencyProto(this).toByteArray()
}
