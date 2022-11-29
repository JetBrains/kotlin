/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto.tcs

import org.jetbrains.kotlin.gradle.idea.proto.Extras
import org.jetbrains.kotlin.gradle.idea.proto.IdeaExtrasProto
import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.IdeaKotlinSourceDependencyProto
import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.ideaKotlinSourceDependencyProto
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinSerializationContext
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency
import org.jetbrains.kotlin.tooling.core.toMutableExtras

internal fun IdeaKotlinSerializationContext.IdeaKotlinSourceDependencyProto(
    dependency: IdeaKotlinSourceDependency
): IdeaKotlinSourceDependencyProto {
    return ideaKotlinSourceDependencyProto {
        this.extras = IdeaExtrasProto(dependency.extras)
        this.type = dependency.type.toProto()
        this.coordinates = IdeaKotlinSourceCoordinatesProto(dependency.coordinates)
    }
}

internal fun IdeaKotlinSerializationContext.IdeaKotlinSourceDependency(
    proto: IdeaKotlinSourceDependencyProto
): IdeaKotlinSourceDependency {
    return IdeaKotlinSourceDependency(
        extras = Extras(proto.extras).toMutableExtras(),
        type = IdeaKotlinSourceDependencyType(proto.type),
        coordinates = IdeaKotlinSourceCoordinates(proto.coordinates),
    )
}
