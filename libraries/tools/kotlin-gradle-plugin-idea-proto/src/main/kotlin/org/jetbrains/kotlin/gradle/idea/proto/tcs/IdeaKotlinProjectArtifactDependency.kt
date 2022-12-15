/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto.tcs

import org.jetbrains.kotlin.gradle.idea.proto.Extras
import org.jetbrains.kotlin.gradle.idea.proto.IdeaExtrasProto
import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.IdeaKotlinProjectArtifactDependencyProto
import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.ideaKotlinProjectArtifactDependencyProto
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinSerializationContext
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinProjectArtifactDependency
import org.jetbrains.kotlin.tooling.core.toMutableExtras

internal fun IdeaKotlinSerializationContext.IdeaKotlinProjectArtifactDependencyProto(
    dependency: IdeaKotlinProjectArtifactDependency
): IdeaKotlinProjectArtifactDependencyProto {
    return ideaKotlinProjectArtifactDependencyProto {
        this.extras = IdeaExtrasProto(dependency.extras)
        this.type = dependency.type.toProto()
        this.coordinates = IdeaKotlinProjectCoordinatesProto(dependency.coordinates)
    }
}

internal fun IdeaKotlinSerializationContext.IdeaKotlinProjectArtifactDependency(
    proto: IdeaKotlinProjectArtifactDependencyProto
): IdeaKotlinProjectArtifactDependency {
    return IdeaKotlinProjectArtifactDependency(
        extras = Extras(proto.extras).toMutableExtras(),
        type = IdeaKotlinSourceDependencyType(proto.type),
        coordinates = IdeaKotlinProjectCoordinates(proto.coordinates),
    )
}
