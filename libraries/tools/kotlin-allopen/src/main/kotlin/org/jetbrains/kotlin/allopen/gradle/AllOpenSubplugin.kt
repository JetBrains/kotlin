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

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.tooling.provider.model.ToolingModelBuilder
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.allopen.gradle.model.builder.AllOpenModelBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.*
import javax.inject.Inject

class AllOpenGradleSubplugin @Inject internal constructor(private val registry: ToolingModelBuilderRegistry) :
    KotlinCompilerPluginSupportPlugin,
    @Suppress("DEPRECATION_ERROR") // implementing to fix KT-39809
    KotlinGradleSubplugin<AbstractCompile> {

    companion object {
        fun getAllOpenExtension(project: Project): AllOpenExtension {
            return project.extensions.getByType(AllOpenExtension::class.java)
        }

        private const val ALLOPEN_ARTIFACT_NAME = "kotlin-allopen"

        private const val ANNOTATION_ARG_NAME = "annotation"
        private const val PRESET_ARG_NAME = "preset"
    }

    override fun apply(target: Project) {
        target.extensions.create("allOpen", AllOpenExtension::class.java)
        registry.register(AllOpenModelBuilder())
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        val allOpenExtension = project.extensions.getByType(AllOpenExtension::class.java)

        return project.provider {
            val options = mutableListOf<SubpluginOption>()

            for (anno in allOpenExtension.myAnnotations) {
                options += SubpluginOption(ANNOTATION_ARG_NAME, anno)
            }

            for (preset in allOpenExtension.myPresets) {
                options += SubpluginOption(PRESET_ARG_NAME, preset)
            }

            options
        }
    }

    override fun getCompilerPluginId() = "org.jetbrains.kotlin.allopen"
    override fun getPluginArtifact(): SubpluginArtifact =
        JetBrainsSubpluginArtifact(artifactId = ALLOPEN_ARTIFACT_NAME)

    //region Stub implementation for legacy API, KT-39809
    internal constructor(): this(object : ToolingModelBuilderRegistry {
        override fun register(p0: ToolingModelBuilder) = Unit
        override fun getBuilder(p0: String): ToolingModelBuilder = error("Method should not be called")
    })

    override fun isApplicable(project: Project, task: AbstractCompile): Boolean = true

    override fun apply(
        project: Project, kotlinCompile: AbstractCompile, javaCompile: AbstractCompile?, variantData: Any?, androidProjectHandler: Any?,
        kotlinCompilation: KotlinCompilation<KotlinCommonOptions>?
    ): List<SubpluginOption> = throw GradleException(
        "This version of the kotlin-allopen Gradle plugin is built for a newer Kotlin version. " +
                "Please use an older version of kotlin-allopen or upgrade the Kotlin Gradle plugin version to make them match."
    )
    //endregion
}
