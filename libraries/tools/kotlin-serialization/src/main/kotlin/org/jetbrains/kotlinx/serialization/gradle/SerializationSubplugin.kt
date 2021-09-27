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

package org.jetbrains.kotlinx.serialization.gradle

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.tooling.provider.model.ToolingModelBuilder
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class SerializationGradleSubplugin :
    KotlinCompilerPluginSupportPlugin,
    @Suppress("DEPRECATION_ERROR") // implementing to fix KT-39809
    KotlinGradleSubplugin<AbstractCompile> {

    companion object {
        const val SERIALIZATION_GROUP_NAME = "org.jetbrains.kotlin"
        const val SERIALIZATION_ARTIFACT_NAME = "kotlin-serialization"
        const val SERIALIZATION_ARTIFACT_UNSHADED_NAME = "kotlin-serialization-unshaded"
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> =
        kotlinCompilation.target.project.provider { emptyList<SubpluginOption>() }

    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact(SERIALIZATION_GROUP_NAME, SERIALIZATION_ARTIFACT_NAME)

    override fun getPluginArtifactForNative(): SubpluginArtifact? =
        SubpluginArtifact(SERIALIZATION_GROUP_NAME, SERIALIZATION_ARTIFACT_UNSHADED_NAME)

    override fun getCompilerPluginId() = "org.jetbrains.kotlinx.serialization"

    //region Stub implementation for legacy API, KT-39809
    override fun isApplicable(project: Project, task: AbstractCompile): Boolean = true

    override fun apply(
        project: Project, kotlinCompile: AbstractCompile, javaCompile: AbstractCompile?, variantData: Any?, androidProjectHandler: Any?,
        kotlinCompilation: KotlinCompilation<KotlinCommonOptions>?
    ): List<SubpluginOption> = throw GradleException(
        "This version of the kotlin-serialization Gradle plugin is built for a newer Kotlin version. " +
                "Please use an older version of kotlin-serialization or upgrade the Kotlin Gradle plugin version to make them match."
    )
    //endregion
}
