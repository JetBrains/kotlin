/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class DataFrameSubplugin : KotlinCompilerPluginSupportPlugin {

    companion object {
        const val DATAFRAME_GROUP_NAME = "org.jetbrains.kotlin"
        const val DATAFRAME_ARTIFACT_NAME = "kotlin-dataframe-compiler-plugin-experimental"

        private const val EXTENSION_NAME = "dataframe"
        private const val POLYMORPHIC_DATA_SCHEMAS_ARG_NAME = "polymorphicDataSchemas"
    }

    override fun apply(target: Project) {
        target.extensions.create(EXTENSION_NAME, DataFrameExtension::class.java)
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        val extension = kotlinCompilation.target.project.extensions.getByType(DataFrameExtension::class.java)
        // Options are reported through the Gradle model, which is also what the IDE imports, so the plugin behaves
        // the same in the editor and in the build. Only non-default values are passed to keep the compiler command
        // line of an unconfigured project unchanged.
        return extension.polymorphicDataSchemas.map { enabled ->
            if (enabled) listOf(SubpluginOption(POLYMORPHIC_DATA_SCHEMAS_ARG_NAME, "true")) else emptyList()
        }
    }

    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact(DATAFRAME_GROUP_NAME, DATAFRAME_ARTIFACT_NAME)

    override fun getCompilerPluginId() = "org.jetbrains.kotlin.dataframe"
}
