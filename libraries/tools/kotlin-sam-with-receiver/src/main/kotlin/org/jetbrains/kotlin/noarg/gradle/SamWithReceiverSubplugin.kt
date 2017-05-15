/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.samWithReceiver.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.AbstractTask
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class SamWithReceiverGradleSubplugin : Plugin<Project> {
    companion object {
        fun isEnabled(project: Project) = project.plugins.findPlugin(SamWithReceiverGradleSubplugin::class.java) != null

        fun getSamWithReceiverExtension(project: Project): SamWithReceiverExtension {
            return project.extensions.getByType(SamWithReceiverExtension::class.java)
        }
    }

    fun Project.getBuildscriptArtifacts(): Set<ResolvedArtifact> =
            buildscript.configurations.findByName("classpath")?.resolvedConfiguration?.resolvedArtifacts ?: emptySet()

    override fun apply(project: Project) {
        val samWithReceiverExtension = project.extensions.create("samWithReceiver", SamWithReceiverExtension::class.java)

        project.afterEvaluate {
            val fqNamesAsString = samWithReceiverExtension.myAnnotations.joinToString(",")
            val presetsAsString = samWithReceiverExtension.myPresets.joinToString(",")
            project.extensions.extraProperties.set("kotlinSamWithReceiverAnnotations", fqNamesAsString)

            val allBuildscriptArtifacts = project.getBuildscriptArtifacts() + project.rootProject.getBuildscriptArtifacts()
            val samWithReceiverCompilerPluginFile = allBuildscriptArtifacts.filter {
                val id = it.moduleVersion.id
                id.group == SamWithReceiverKotlinGradleSubplugin.SAM_WITH_RECEIVER_GROUP_NAME
                && id.name == SamWithReceiverKotlinGradleSubplugin.SAM_WITH_RECEIVER_ARTIFACT_NAME
            }.firstOrNull()?.file?.absolutePath ?: ""

            open class TaskForSamWithReceiver : AbstractTask()
            project.tasks.add(project.tasks.create("samWithReceiverDataStorageTask", TaskForSamWithReceiver::class.java).apply {
                isEnabled = false
                description = "Supported annotations: " + fqNamesAsString +
                              "; Presets: $presetsAsString" +
                              "; Compiler plugin classpath: $samWithReceiverCompilerPluginFile"
            })
        }
    }
}

class SamWithReceiverKotlinGradleSubplugin : KotlinGradleSubplugin<AbstractCompile> {
    companion object {
        val SAM_WITH_RECEIVER_GROUP_NAME = "org.jetbrains.kotlin"
        val SAM_WITH_RECEIVER_ARTIFACT_NAME = "kotlin-sam-with-receiver"

        private val ANNOTATION_ARG_NAME = "annotation"
        private val PRESET_ARG_NAME = "preset"
    }

    override fun isApplicable(project: Project, task: AbstractCompile) = SamWithReceiverGradleSubplugin.isEnabled(project)

    override fun apply(
            project: Project,
            kotlinCompile: AbstractCompile,
            javaCompile: AbstractCompile,
            variantData: Any?,
            androidProjectHandler: Any?,
            javaSourceSet: SourceSet?
    ): List<SubpluginOption> {
        if (!SamWithReceiverGradleSubplugin.isEnabled(project)) return emptyList()

        val samWithReceiverExtension = project.extensions.findByType(SamWithReceiverExtension::class.java) ?: return emptyList()

        val options = mutableListOf<SubpluginOption>()

        for (anno in samWithReceiverExtension.myAnnotations) {
            options += SubpluginOption(ANNOTATION_ARG_NAME, anno)
        }

        for (preset in samWithReceiverExtension.myPresets) {
            options += SubpluginOption(PRESET_ARG_NAME, preset)
        }

        return options
    }

    override fun getArtifactName() = "kotlin-sam-with-receiver"
    override fun getGroupName() = "org.jetbrains.kotlin"
    override fun getCompilerPluginId() = "org.jetbrains.kotlin.samWithReceiver"
}