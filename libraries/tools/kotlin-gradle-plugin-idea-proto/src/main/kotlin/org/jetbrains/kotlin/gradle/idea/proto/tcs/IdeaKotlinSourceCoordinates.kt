package org.jetbrains.kotlin.gradle.idea.proto.tcs

import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.IdeaKotlinSourceCoordinatesProto
import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.ideaKotlinSourceCoordinatesProto
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceCoordinates

internal fun IdeaKotlinSourceCoordinatesProto(
    coordinates: IdeaKotlinSourceCoordinates
): IdeaKotlinSourceCoordinatesProto {
    return ideaKotlinSourceCoordinatesProto {
        this.buildId = coordinates.buildId
        this.projectPath = coordinates.projectPath
        this.projectName = coordinates.projectName
        this.sourceSetName = coordinates.sourceSetName
    }
}

internal fun IdeaKotlinSourceCoordinates(
    proto: IdeaKotlinSourceCoordinatesProto
): IdeaKotlinSourceCoordinates {
    return IdeaKotlinSourceCoordinates(
        buildId = proto.buildId,
        projectPath = proto.projectPath,
        projectName = proto.projectName,
        sourceSetName = proto.sourceSetName
    )
}