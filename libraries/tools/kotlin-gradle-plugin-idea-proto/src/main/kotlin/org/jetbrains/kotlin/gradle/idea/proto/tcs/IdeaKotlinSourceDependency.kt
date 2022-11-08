/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto.tcs

import org.jetbrains.kotlin.gradle.idea.proto.Extras
import org.jetbrains.kotlin.gradle.idea.proto.IdeaExtrasProto
import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.IdeaKotlinSourceDependencyProto
import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.ideaKotlinSourceDependencyProto
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaSerializationContext
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency
import org.jetbrains.kotlin.tooling.core.toMutableExtras

internal fun IdeaSerializationContext.IdeaKotlinSourceDependencyProto(
    dependency: IdeaKotlinSourceDependency
): IdeaKotlinSourceDependencyProto {
    return ideaKotlinSourceDependencyProto {
        this.extras = IdeaExtrasProto(dependency.extras)
        this.type = when (dependency.type) {
            IdeaKotlinSourceDependency.Type.Regular -> IdeaKotlinSourceDependencyProto.Type.REGULAR
            IdeaKotlinSourceDependency.Type.Friend -> IdeaKotlinSourceDependencyProto.Type.FRIEND
            IdeaKotlinSourceDependency.Type.DependsOn -> IdeaKotlinSourceDependencyProto.Type.DEPENDS_ON
        }
        this.coordinates = IdeaKotlinSourceCoordinatesProto(dependency.coordinates)
    }
}

internal fun IdeaSerializationContext.IdeaKotlinSourceDependency(
    proto: IdeaKotlinSourceDependencyProto
): IdeaKotlinSourceDependency {
    return IdeaKotlinSourceDependency(
        extras = Extras(proto.extras).toMutableExtras(),
        type = when (proto.type) {
            IdeaKotlinSourceDependencyProto.Type.REGULAR -> IdeaKotlinSourceDependency.Type.Regular
            IdeaKotlinSourceDependencyProto.Type.FRIEND -> IdeaKotlinSourceDependency.Type.Friend
            IdeaKotlinSourceDependencyProto.Type.DEPENDS_ON -> IdeaKotlinSourceDependency.Type.DependsOn
            IdeaKotlinSourceDependencyProto.Type.UNRECOGNIZED, null -> {
                logger.warn("Unexpected ${IdeaKotlinSourceDependencyProto.Type::class.java.name}: ${proto.type}")
                IdeaKotlinSourceDependency.Type.Regular
            }
        },
        coordinates = IdeaKotlinSourceCoordinates(proto.coordinates),
    )
}