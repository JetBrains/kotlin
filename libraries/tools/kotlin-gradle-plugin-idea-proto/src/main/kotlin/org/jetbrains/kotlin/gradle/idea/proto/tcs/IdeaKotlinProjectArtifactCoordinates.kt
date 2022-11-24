/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto.tcs

import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.IdeaKotlinProjectArtifactCoordinatesProto
import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.ideaKotlinProjectArtifactCoordinatesProto
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinProjectArtifactCoordinates
import java.io.File

internal fun IdeaKotlinProjectArtifactCoordinatesProto(
    coordinates: IdeaKotlinProjectArtifactCoordinates
): IdeaKotlinProjectArtifactCoordinatesProto {
    return ideaKotlinProjectArtifactCoordinatesProto {
        this.project = IdeaKotlinProjectCoordinatesProto(coordinates.project)
        this.artifactFile = coordinates.artifactFile.path
    }
}

internal fun IdeaKotlinProjectArtifactCoordinates(proto: IdeaKotlinProjectArtifactCoordinatesProto): IdeaKotlinProjectArtifactCoordinates {
    return IdeaKotlinProjectArtifactCoordinates(
        project = IdeaKotlinProjectCoordinates(proto.project),
        artifactFile = File(proto.artifactFile)
    )
}
