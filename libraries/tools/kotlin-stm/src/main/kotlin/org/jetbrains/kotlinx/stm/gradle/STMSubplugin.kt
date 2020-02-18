/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.stm.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class STMGradleSubplugin : Plugin<Project> {
    companion object {
        fun isEnabled(project: Project) = project.plugins.findPlugin(STMGradleSubplugin::class.java) != null
    }

    override fun apply(project: Project) {
        // nothing here
    }
}

class STMKotlinGradleSubplugin : KotlinGradleSubplugin<AbstractCompile> {
    companion object {
        const val STM_GROUP_NAME = "org.jetbrains.kotlin"
        const val STM_ARTIFACT_NAME = "kotlin-stm"
        const val STM_ARTIFACT_UNSHADED_NAME = "kotlin-stm-unshaded"
    }

    override fun isApplicable(project: Project, task: AbstractCompile): Boolean =
        STMGradleSubplugin.isEnabled(project)

    override fun apply(
        project: Project,
        kotlinCompile: AbstractCompile,
        javaCompile: AbstractCompile?,
        variantData: Any?,
        androidProjectHandler: Any?,
        kotlinCompilation: KotlinCompilation<*>?
    ): List<SubpluginOption> {
        return emptyList()
    }

    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact(STM_GROUP_NAME, STM_ARTIFACT_NAME)

    override fun getNativeCompilerPluginArtifact(): SubpluginArtifact? =
        SubpluginArtifact(STM_GROUP_NAME, STM_ARTIFACT_UNSHADED_NAME)

    override fun getCompilerPluginId() = "org.jetbrains.kotlinx.stm"
}
