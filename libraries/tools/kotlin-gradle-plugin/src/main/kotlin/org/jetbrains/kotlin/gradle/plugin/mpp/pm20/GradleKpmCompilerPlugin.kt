/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.tasks.CompilerPluginOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerPluginData
import org.jetbrains.kotlin.gradle.utils.newProperty
import org.jetbrains.kotlin.project.model.*

internal fun Project.compilerPluginProviderForMetadata(
    fragment: KotlinGradleFragment,
    compilationData: KotlinCommonFragmentMetadataCompilationData
) = compilerPluginDataProvider(compilationData, fragment::metadataCompilationPluginData)

internal fun Project.compilerPluginProviderForNativeMetadata(
    fragment: KotlinGradleFragment,
    compilationData: KotlinNativeFragmentMetadataCompilationData
) = compilerPluginDataProvider(compilationData, fragment::nativeMetadataCompilationPluginData)

internal fun Project.compilerPluginProviderForPlatformCompilation(
    variant: KotlinGradleVariant,
    compilationData: KotlinCompilationData<*>
) = compilerPluginDataProvider(compilationData, variant::platformCompilationPluginData)

internal fun KotlinCompilationData<*>.pluginClasspathConfigurationName() = "${compileKotlinTaskName}PluginClasspath"

private fun Project.compilerPluginDataProvider(
    compilationData: KotlinCompilationData<*>,
    pluginDataList: () -> List<PluginData>
): Provider<KotlinCompilerPluginData> {
    val configurationName = compilationData.pluginClasspathConfigurationName()
    val pluginClasspathConfiguration =
        configurations.maybeCreate(configurationName).apply {
            isCanBeConsumed = false
            isCanBeResolved = true
            isVisible = false
        }

    val pluginOptions = CompilerPluginOptions()
    return newProperty {
        for (pluginData in pluginDataList()) {
            dependencies.add(pluginClasspathConfiguration.name, pluginData.artifact.toGradleCoordinates(project))
            pluginData.options.forEach { (key, value) ->
                pluginOptions.addPluginArgument(pluginData.pluginId, SubpluginOption(key, value))
            }
        }

        KotlinCompilerPluginData(
            classpath = pluginClasspathConfiguration,
            options = pluginOptions
        )
    }.apply { finalizeValueOnRead() }
}

private fun PluginData.ArtifactCoordinates.toGradleCoordinates(project: Project): String =
    "$group:$artifact:${version ?: project.getKotlinPluginVersion()}"
