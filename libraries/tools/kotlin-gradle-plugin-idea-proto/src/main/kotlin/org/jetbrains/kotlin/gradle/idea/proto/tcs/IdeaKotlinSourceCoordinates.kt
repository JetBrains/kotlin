package org.jetbrains.kotlin.gradle.idea.proto.tcs

import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.IdeaKotlinSourceCoordinatesProto
import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.ideaKotlinSourceCoordinatesProto
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceCoordinates

internal fun IdeaKotlinSourceCoordinatesProto(
    coordinates: IdeaKotlinSourceCoordinates
): IdeaKotlinSourceCoordinatesProto {
    return ideaKotlinSourceCoordinatesProto {
        this.project = IdeaKotlinProjectCoordinatesProto(coordinates.project)
        this.sourceSetName = coordinates.sourceSetName
    }
}

internal fun IdeaKotlinSourceCoordinates(
    proto: IdeaKotlinSourceCoordinatesProto
): IdeaKotlinSourceCoordinates {
    return IdeaKotlinSourceCoordinates(
        project = IdeaKotlinProjectCoordinates(proto.project),
        sourceSetName = proto.sourceSetName
    )
}
