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
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.internal.AbstractTask
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

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
        val SERIALIZATION_GROUP_NAME = "org.jetbrains.kotlinx"
        val SERIALIZATION_ARTIFACT_NAME = "kotlinx-gradle-serialization-plugin"
    }

    override fun isApplicable(project: Project, task: AbstractCompile) = SerializationGradleSubplugin.isEnabled(project)

    override fun apply(
            project: Project, kotlinCompile: AbstractCompile, javaCompile: AbstractCompile, variantData: Any?, androidProjectHandler: Any?, javaSourceSet: SourceSet?)
            : List<SubpluginOption> {
        if (!SerializationGradleSubplugin.isEnabled(project)) return emptyList()
        val options = mutableListOf<SubpluginOption>()
        // nothing here
        return options
    }

    override fun getGroupName() = SERIALIZATION_GROUP_NAME
    override fun getArtifactName() = SERIALIZATION_ARTIFACT_NAME
    override fun getCompilerPluginId() = "org.jetbrains.kotlinx.serialization"
}