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
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.noarg.gradle.model.builder.NoArgModelBuilder
import javax.inject.Inject

class NoArgGradleSubplugin @Inject internal constructor(private val registry: ToolingModelBuilderRegistry) : Plugin<Project> {
    companion object {
        fun isEnabled(project: Project) = project.plugins.findPlugin(NoArgGradleSubplugin::class.java) != null

        fun getNoArgExtension(project: Project): NoArgExtension {
            return project.extensions.getByType(NoArgExtension::class.java)
        }
    }

    override fun apply(project: Project) {
        project.extensions.create("noArg", NoArgExtension::class.java)
        registry.register(NoArgModelBuilder())
    }
}

class NoArgKotlinGradleSubplugin : KotlinGradleSubplugin<AbstractCompile> {
    companion object {
        const val NOARG_ARTIFACT_NAME = "kotlin-noarg"

        private val ANNOTATION_ARG_NAME = "annotation"
        private val PRESET_ARG_NAME = "preset"
        private val INVOKE_INITIALIZERS_ARG_NAME = "invokeInitializers"
    }

    override fun isApplicable(project: Project, task: AbstractCompile) = NoArgGradleSubplugin.isEnabled(project)

    override fun apply(
        project: Project,
        kotlinCompile: AbstractCompile,
        javaCompile: AbstractCompile?,
        variantData: Any?,
        androidProjectHandler: Any?,
        kotlinCompilation: KotlinCompilation<*>?
    ): List<SubpluginOption> {
        if (!NoArgGradleSubplugin.isEnabled(project)) return emptyList()

        val noArgExtension = project.extensions.findByType(NoArgExtension::class.java) ?: return emptyList()

        val options = mutableListOf<SubpluginOption>()

        for (anno in noArgExtension.myAnnotations) {
            options += SubpluginOption(ANNOTATION_ARG_NAME, anno)
        }

        for (preset in noArgExtension.myPresets) {
            options += SubpluginOption(PRESET_ARG_NAME, preset)
        }

        if (noArgExtension.invokeInitializers) {
            options += SubpluginOption(INVOKE_INITIALIZERS_ARG_NAME, "true")
        }

        return options
    }

    override fun getCompilerPluginId() = "org.jetbrains.kotlin.noarg"
    override fun getPluginArtifact(): SubpluginArtifact =
        JetBrainsSubpluginArtifact(artifactId = NOARG_ARTIFACT_NAME)
}