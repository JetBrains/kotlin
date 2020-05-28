/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginInMultipleProjectsHolder
import org.jetbrains.kotlin.gradle.plugin.MULTIPLE_KOTLIN_PLUGINS_LOADED_WARNING

internal object MultiplePluginDeclarationDetector {
    private val pluginInMultipleProjectsHolder = KotlinPluginInMultipleProjectsHolder(
        differentVersionsInDifferentProject = false
    )

    fun detect(project: Project) {
        pluginInMultipleProjectsHolder
            .addProject(project)

        if (pluginInMultipleProjectsHolder.isInMultipleProjects(project)) {
            error(MULTIPLE_KOTLIN_PLUGINS_LOADED_WARNING)
        }
    }
}