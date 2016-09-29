/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.allopen.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class AllOpenGradleSubplugin : Plugin<Project> {
    companion object {
        fun isEnabled(project: Project) = project.plugins.findPlugin(AllOpenGradleSubplugin::class.java) != null

        fun getAllOpenExtension(project: Project): AllOpenExtension {
            return project.extensions.getByType(AllOpenExtension::class.java)
        }
    }

    override fun apply(project: Project) {
        project.extensions.create("allOpen", AllOpenExtension::class.java)
    }
}

class AllOpenKotlinGradleSubplugin : KotlinGradleSubplugin<AbstractCompile> {
    private companion object {
        val ANNOTATIONS_ARG_NAME = "annotation"
    }

    override fun isApplicable(project: Project, task: AbstractCompile) = AllOpenGradleSubplugin.isEnabled(project)

    override fun apply(
            project: Project,
            kotlinCompile: AbstractCompile,
            javaCompile: AbstractCompile,
            variantData: Any?,
            javaSourceSet: SourceSet?
    ): List<SubpluginOption> {
        if (!AllOpenGradleSubplugin.isEnabled(project)) return emptyList()

        val allOpenExtension = project.extensions.findByType(AllOpenExtension::class.java) ?: return emptyList()

        val options = mutableListOf<SubpluginOption>()

        for (anno in allOpenExtension.myAnnotations) {
            options += SubpluginOption(ANNOTATIONS_ARG_NAME, anno)
        }

        return options
    }

    override fun getArtifactName() = "kotlin-allopen"
    override fun getGroupName() = "org.jetbrains.kotlin"
    override fun getPluginName() = "org.jetbrains.kotlin.allopen"
}