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

package org.jetbrains.kotlin.noarg.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.AbstractTask
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class NoArgGradleSubplugin : Plugin<Project> {
    companion object {
        fun isEnabled(project: Project) = project.plugins.findPlugin(NoArgGradleSubplugin::class.java) != null

        fun getNoArgExtension(project: Project): NoArgExtension {
            return project.extensions.getByType(NoArgExtension::class.java)
        }
    }

    override fun apply(project: Project) {
        val noArgExtension = project.extensions.create("noArg", NoArgExtension::class.java)

        project.afterEvaluate {
            val fqNamesAsString = noArgExtension.myAnnotations.joinToString(",")
            project.extensions.extraProperties.set("kotlinNoArgAnnotations", fqNamesAsString)

            open class TaskForAllOpen : AbstractTask()
            project.tasks.add(project.tasks.create("noArgDataStorageTask", TaskForAllOpen::class.java).apply {
                isEnabled = false
                description = "Supported no-arg annotations: " + fqNamesAsString
            })
        }
    }
}

class NoArgKotlinGradleSubplugin : KotlinGradleSubplugin<AbstractCompile> {
    companion object {
        val NOARG_GROUP_NAME = "org.jetbrains.kotlin"
        val NOARG_ARTIFACT_NAME = "kotlin-noarg"

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
        if (!NoArgGradleSubplugin.isEnabled(project)) return emptyList()

        val allOpenExtension = project.extensions.findByType(AllOpenExtension::class.java) ?: return emptyList()

        val options = mutableListOf<SubpluginOption>()

        for (anno in allOpenExtension.myAnnotations) {
            options += SubpluginOption(ANNOTATIONS_ARG_NAME, anno)
        }

        return options
    }

    override fun getArtifactName() = "kotlin-noarg"
    override fun getGroupName() = "org.jetbrains.kotlin"
    override fun getPluginName() = "org.jetbrains.kotlin.noarg"
}