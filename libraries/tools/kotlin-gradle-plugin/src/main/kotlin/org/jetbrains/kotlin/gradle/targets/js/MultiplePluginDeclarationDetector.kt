/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.jetbrains.kotlin.gradle.plugin.BuildFinishedListenerService
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginInMultipleProjectsHolder
import org.jetbrains.kotlin.gradle.plugin.MULTIPLE_KOTLIN_PLUGINS_LOADED_WARNING
import org.jetbrains.kotlin.gradle.utils.isConfigurationCacheAvailable

internal class MultiplePluginDeclarationDetector
private constructor() {
    private val pluginInMultipleProjectsHolder = KotlinPluginInMultipleProjectsHolder(
        trackPluginVersionsSeparately = false
    )

    fun detect(project: Project) {
        pluginInMultipleProjectsHolder
            .addProject(project)

        if (pluginInMultipleProjectsHolder.isInMultipleProjects(project)) {
            error(MULTIPLE_KOTLIN_PLUGINS_LOADED_WARNING)
        }
    }

    // We can't use Kotlin object because we need new instance on each Gradle rebuild
    // But if we inside Gradle daemon, Kotlin object will be shared between builds
    companion object {
        @field:Volatile
        private var instance: MultiplePluginDeclarationDetector? = null

        @JvmStatic
        @Synchronized
        private fun getInstance(gradle: Gradle): MultiplePluginDeclarationDetector {
            if (instance != null) {
                return instance!!
            }

            val detector = MultiplePluginDeclarationDetector()
            instance = detector

            BuildFinishedListenerService.getInstance(gradle).onClose { instance = null }

            return detector
        }

        fun detect(project: Project) {
            getInstance(project.gradle).detect(project)
        }
    }
}
