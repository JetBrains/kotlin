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
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.internal.AbstractTask
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

    fun Project.getBuildscriptArtifacts(): Set<ResolvedArtifact> =
            buildscript.configurations.findByName("classpath")?.resolvedConfiguration?.resolvedArtifacts ?: emptySet()

    override fun apply(project: Project) {
        val allOpenExtension = project.extensions.create("allOpen", AllOpenExtension::class.java)

        project.afterEvaluate {
            val fqNamesAsString = allOpenExtension.myAnnotations.joinToString(",")
            project.extensions.extraProperties.set("kotlinAllOpenAnnotations", fqNamesAsString)

            val allBuildscriptArtifacts = project.getBuildscriptArtifacts() + project.rootProject.getBuildscriptArtifacts()
            val allOpenCompilerPluginFile = allBuildscriptArtifacts.filter {
                val id = it.moduleVersion.id
                id.group == AllOpenKotlinGradleSubplugin.ALLOPEN_GROUP_NAME
                        && id.name == AllOpenKotlinGradleSubplugin.ALLOPEN_ARTIFACT_NAME
            }.firstOrNull()?.file?.absolutePath ?: ""

            open class TaskForAllOpen : AbstractTask()
            project.tasks.add(project.tasks.create("allOpenDataStorageTask", TaskForAllOpen::class.java).apply {
                isEnabled = false
                description = "Supported annotations: " + fqNamesAsString +
                        "; Compiler plugin classpath: $allOpenCompilerPluginFile"
            })
        }
    }
}

class AllOpenKotlinGradleSubplugin : KotlinGradleSubplugin<AbstractCompile> {
    companion object {
        val ALLOPEN_GROUP_NAME = "org.jetbrains.kotlin"
        val ALLOPEN_ARTIFACT_NAME = "kotlin-allopen"

        private val ANNOTATIONS_ARG_NAME = "annotation"
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

    override fun getGroupName() = ALLOPEN_GROUP_NAME
    override fun getArtifactName() = ALLOPEN_ARTIFACT_NAME
    override fun getCompilerPluginId() = "org.jetbrains.kotlin.allopen"
}