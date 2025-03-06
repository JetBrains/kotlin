/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.gradle

import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class DataFrameSubplugin : KotlinCompilerPluginSupportPlugin {

    companion object {
        const val DATAFRAME_GROUP_NAME = "org.jetbrains.kotlin"
        const val DATAFRAME_ARTIFACT_NAME = "kotlin-dataframe-compiler-plugin"
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> =
        kotlinCompilation.target.project.provider { emptyList() }

    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact(DATAFRAME_GROUP_NAME, DATAFRAME_ARTIFACT_NAME)

    override fun getCompilerPluginId() = "org.jetbrains.kotlin.dataframe"
}