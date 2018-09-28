/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.plugin.experimental.plugins

import org.gradle.api.Named
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformImplementationPluginBase

class KotlinPlatformNativePlugin : KotlinPlatformImplementationPluginBase("native") {

    override fun apply(project: Project) = with(project) {
        pluginManager.apply(KotlinNativePlugin::class.java)
        // This configuration is necessary for correct work of the base platform plugin.
        configurations.create("compile")
        super.apply(project)
    }

    override fun addCommonSourceSetToPlatformSourceSet(commonSourceSet: Named, platformProject: Project) {
        val platformSourceSet = platformProject.kotlinNativeSourceSets.findByName(commonSourceSet.name)
        val commonSources = getKotlinSourceDirectorySetSafe(commonSourceSet)
        if (platformSourceSet != null && commonSources != null) {
            platformSourceSet.commonSources.from(commonSources)
        } else {
            platformProject.logger.warn("""
                Cannot add a common source set '${commonSourceSet.name}' in the project '${platformProject.path}'.
                No such platform source set or the common source set has no Kotlin sources.
            """.trimIndent())
        }
    }

    override fun namedSourceSetsContainer(project: Project) = project.kotlinNativeSourceSets
}
