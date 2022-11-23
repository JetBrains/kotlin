/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide

import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceCoordinates
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.currentBuildId
import org.jetbrains.kotlin.gradle.plugin.sources.project

internal fun IdeaKotlinSourceCoordinates(sourceSet: KotlinSourceSet): IdeaKotlinSourceCoordinates {
    return IdeaKotlinSourceCoordinates(
        buildId = sourceSet.project.currentBuildId().toString(),
        projectPath = sourceSet.project.path,
        projectName = sourceSet.project.name,
        sourceSetName = sourceSet.name
    )
}