/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto.tcs

import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.IdeaKotlinProjectCoordinatesProto
import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.ideaKotlinProjectCoordinatesProto
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinProjectCoordinates

internal fun IdeaKotlinProjectCoordinatesProto(coordinates: IdeaKotlinProjectCoordinates): IdeaKotlinProjectCoordinatesProto {
    return ideaKotlinProjectCoordinatesProto {
        this.buildId = coordinates.buildId
        this.projectPath = coordinates.projectPath
        this.projectName = coordinates.projectName
    }
}

internal fun IdeaKotlinProjectCoordinates(proto: IdeaKotlinProjectCoordinatesProto): IdeaKotlinProjectCoordinates {
    return IdeaKotlinProjectCoordinates(
        buildId = proto.buildId,
        projectPath = proto.projectPath,
        projectName = proto.projectName
    )
}

