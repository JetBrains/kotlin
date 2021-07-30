/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.internal.reorderPluginClasspathDependencies
import org.jetbrains.kotlin.gradle.plugin.FilesSubpluginOption
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.tasks.CompilerPluginOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerPluginData
import org.jetbrains.kotlin.gradle.utils.newProperty
import org.jetbrains.kotlin.project.model.*
import java.io.File

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
    return newProperty {
        val configurationName = compilationData.pluginClasspathConfigurationName()
        val builder = CompilerPluginOptionsBuilder(project, configurationName)
        builder += pluginDataList()
        builder.build()
    }.apply { disallowUnsafeRead() }
}

internal class CompilerPluginOptionsBuilder(
    private val project: Project,
    private val configurationName: String
) {
    private val pluginOptions = CompilerPluginOptions()
    private val artifacts = mutableListOf<String>()
    private val gradleInputs = mutableMapOf<String, MutableList<String>>()
    private val gradleInputFiles = mutableSetOf<File>()
    private val gradleOutputFiles = mutableSetOf<File>()

    operator fun plusAssign(pluginData: PluginData) {
        artifacts += pluginData.artifact.toGradleCoordinates()

        for (option in pluginData.options) {
            pluginOptions.addPluginArgument(pluginData.pluginId, option.toSubpluginOption())

            if (!option.isTransient) {
                addToInputsOutputs(pluginData.pluginId, option)
            }
        }
    }

    operator fun plusAssign(pluginDataCollection: Collection<PluginData>) {
        for (pluginData in pluginDataCollection) {
            this += pluginData
        }
    }

    private fun addToInputsOutputs(pluginId: String, option: PluginOption) {
        when (option) {
            is FilesOption ->
                if (option.isOutput) {
                    gradleOutputFiles += option.files
                } else {
                    gradleInputFiles += option.files
                }
            is StringOption -> gradleInputs
                .getOrPut("${pluginId}.${option.key}") { mutableListOf() }
                .add(option.value)
        }
    }

    fun build(): KotlinCompilerPluginData {
        val pluginClasspathConfiguration =
            project.configurations.maybeCreate(configurationName).apply {
                isCanBeConsumed = false
                isCanBeResolved = true
                isVisible = false
                reorderPluginClasspathDependencies()
            }
        artifacts.forEach { project.dependencies.add(configurationName, it) }

        return KotlinCompilerPluginData(
            classpath = pluginClasspathConfiguration,
            options = pluginOptions,
            inputsOutputsState = KotlinCompilerPluginData.InputsOutputsState(
                inputs = gradleInputs.flattenWithIndex(),
                inputFiles = gradleInputFiles,
                outputFiles = gradleOutputFiles
            )
        )
    }

    private fun Map<String, List<String>>.flattenWithIndex(): Map<String, String> {
        val result = mutableMapOf<String, String>()

        for ((key, values) in this) {
            for ((index, value) in values.withIndex()) {
                result["${key}.$index"] = value
            }
        }

        return result
    }

    private fun PluginOption.toSubpluginOption() = when (this) {
        is FilesOption -> FilesSubpluginOption(key, files)
        is StringOption -> SubpluginOption(key, value)
    }

    private fun PluginData.ArtifactCoordinates.toGradleCoordinates(): String =
        listOfNotNull(group, artifact, version).joinToString(":")
}
