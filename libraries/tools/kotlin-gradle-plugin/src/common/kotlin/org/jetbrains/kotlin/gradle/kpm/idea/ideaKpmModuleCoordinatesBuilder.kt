/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmModule
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.currentBuildId

internal fun IdeaKpmModuleCoordinates(module: GradleKpmModule): IdeaKpmModuleCoordinates {
    return IdeaKpmModuleCoordinatesImpl(
        buildId = module.project.currentBuildId().name,
        projectPath = module.project.path,
        projectName = module.project.name,
        moduleName = module.name,
        moduleClassifier = module.moduleClassifier
    )
}
