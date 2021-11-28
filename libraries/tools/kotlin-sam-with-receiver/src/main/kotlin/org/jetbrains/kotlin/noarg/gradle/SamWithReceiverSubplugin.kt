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

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.tooling.provider.model.ToolingModelBuilder
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.noarg.gradle.model.builder.SamWithReceiverModelBuilder
import javax.inject.Inject

class SamWithReceiverGradleSubplugin @Inject internal constructor(private val registry: ToolingModelBuilderRegistry) :
    KotlinCompilerPluginSupportPlugin,
    @Suppress("DEPRECATION_ERROR") // implementing to fix KT-39809
    KotlinGradleSubplugin<AbstractCompile> {

    override fun apply(target: Project) {
        target.extensions.create("samWithReceiver", SamWithReceiverExtension::class.java)
        registry.register(SamWithReceiverModelBuilder())
    }

    companion object {
        const val SAM_WITH_RECEIVER_ARTIFACT_NAME = "kotlin-sam-with-receiver"

        private const val ANNOTATION_ARG_NAME = "annotation"
        private const val PRESET_ARG_NAME = "preset"
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        val samWithReceiverExtension =
            project.extensions.findByType(SamWithReceiverExtension::class.java) ?: return project.provider { emptyList<SubpluginOption>() }

        return project.provider<List<SubpluginOption>> {
            val options = mutableListOf<SubpluginOption>()

            for (anno in samWithReceiverExtension.myAnnotations) {
                options += SubpluginOption(ANNOTATION_ARG_NAME, anno)
            }

            for (preset in samWithReceiverExtension.myPresets) {
                options += SubpluginOption(PRESET_ARG_NAME, preset)
            }

            options
        }
    }

    override fun getCompilerPluginId() = "org.jetbrains.kotlin.samWithReceiver"
    override fun getPluginArtifact(): SubpluginArtifact =
        JetBrainsSubpluginArtifact(artifactId = SAM_WITH_RECEIVER_ARTIFACT_NAME)

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
        "This version of the kotlin-sam-with-receiver Gradle plugin is built for a newer Kotlin version. " +
                "Please use an older version of kotlin-sam-with-receiver or upgrade the Kotlin Gradle plugin version to make them match."
    )
    //endregion
}
