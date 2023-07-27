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
        this.buildName = coordinates.buildName
        this.buildPath = coordinates.buildPath
        this.projectPath = coordinates.projectPath
        this.projectName = coordinates.projectName
    }
}

internal fun IdeaKotlinProjectCoordinates(proto: IdeaKotlinProjectCoordinatesProto): IdeaKotlinProjectCoordinates {
    return if (proto.hasBuildPath()) IdeaKotlinProjectCoordinates(
        buildName = proto.buildName,
        buildPath = proto.buildPath,
        projectPath = proto.projectPath,
        projectName = proto.projectName
    )
    /*
    Coordinates were encoded w/o 'buildPath'.
    This can happen if e.g. the data was produced by an older Kotlin Gradle Plugin (before 1.9.20),
    or if the Kotlin Gradle Plugin was used with older Gradle versions (before 8.2), where this information was not present.
    In this case, we will create the coordinates using the approximation used before 1.9.20: buildName is used as 'buildId'
     */
    else @Suppress("DEPRECATION") IdeaKotlinProjectCoordinates(
        buildId = proto.buildName,
        projectPath = proto.projectPath,
        projectName = proto.projectName
    )
}

