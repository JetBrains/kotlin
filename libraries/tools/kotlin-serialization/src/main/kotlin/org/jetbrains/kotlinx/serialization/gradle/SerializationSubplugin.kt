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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile

class SerializationGradleSubplugin : Plugin<Project> {
    companion object {
        fun isEnabled(project: Project) = project.plugins.findPlugin(SerializationGradleSubplugin::class.java) != null
    }

    override fun apply(project: Project) {
        // nothing here
    }
}

class SerializationKotlinGradleSubplugin : KotlinGradleSubplugin<AbstractCompile> {
    companion object {
        const val SERIALIZATION_GROUP_NAME = "org.jetbrains.kotlin"
        const val SERIALIZATION_ARTIFACT_NAME = "kotlin-serialization"
        const val SERIALIZATION_ARTIFACT_UNSHADED_NAME = "kotlin-serialization-unshaded"
    }

    private var useUnshaded = false

    override fun isApplicable(project: Project, task: AbstractCompile): Boolean {
        if (!SerializationGradleSubplugin.isEnabled(project)) return false
        // Kotlin/Native task is not an AbstractKotlinCompile and uses unshaded compiler
        if (task !is AbstractKotlinCompile<*>) useUnshaded = true
        return true
    }

    override fun apply(
        project: Project,
        kotlinCompile: AbstractCompile,
        javaCompile: AbstractCompile?,
        variantData: Any?,
        androidProjectHandler: Any?,
        kotlinCompilation: KotlinCompilation?
    ): List<SubpluginOption> {
        return emptyList()
    }


    override fun getPluginArtifact(): SubpluginArtifact {
        val artifact = if (useUnshaded) SERIALIZATION_ARTIFACT_UNSHADED_NAME else SERIALIZATION_ARTIFACT_NAME
        return SubpluginArtifact(SERIALIZATION_GROUP_NAME, artifact)
    }

    override fun getCompilerPluginId() = "org.jetbrains.kotlinx.serialization"
}
