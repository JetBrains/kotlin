/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl

import org.gradle.api.plugins.BasePlugin
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.artifacts.klibOutputDirectory
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.topLevelExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationInfo
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.SubpluginEnvironment
import org.jetbrains.kotlin.gradle.plugin.launchInStage
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.enabledOnCurrentHost
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.registerTask

/**
 * Will register (and configure) the corresponding [KotlinNativeCompile] task for a given
 * [AbstractKotlinNativeCompilation] (which includes shared native metadata and 'platform compilations')
 */
internal val KotlinCreateNativeCompileTasksSideEffect = KotlinCompilationSideEffect<AbstractKotlinNativeCompilation> { compilation ->
    val project = compilation.project
    val extension = project.topLevelExtension
    val compilationInfo = KotlinCompilationInfo(compilation)
    val isMetadataCompilation = compilationInfo.compilation is KotlinMetadataCompilation<*>

    val kotlinNativeCompile = project.registerTask<KotlinNativeCompile>(
        compilation.compileKotlinTaskName,
        @Suppress("DEPRECATION")
        listOf(compilationInfo, compilation.compilerOptions.options)
    ) { task ->
        task.group = BasePlugin.BUILD_GROUP
        task.description = "Compiles a klibrary from the '${compilationInfo.compilationName}' " +
                "compilation in target '${compilationInfo.targetDisambiguationClassifier}'."
        task.enabled = compilation.konanTarget.enabledOnCurrentHost

        task.destinationDirectory.set(project.klibOutputDirectory(compilationInfo).dir("klib"))
        task.runViaBuildToolsApi.value(false).disallowChanges() // K/N is not yet supported

        task.explicitApiMode.value(
            project.providers.provider {
                // Plugin explicitly does not configures 'explicitApi' mode for test sources
                // compilation, as test sources are not published
                if (compilationInfo.isMain) {
                    extension.explicitApi
                } else {
                    ExplicitApiMode.Disabled
                }
            }
        ).finalizeValueOnRead()

        // for metadata tasks we should provide unpacked klib
        task.produceUnpackedKlib.set(isMetadataCompilation)
    }

    compilationInfo.classesDirs.from(kotlinNativeCompile.map { it.outputFile })
    project.tasks.named(compilation.compileAllTaskName).dependsOn(kotlinNativeCompile)
    project.tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME).dependsOn(kotlinNativeCompile)
    compilation.addCompilerPlugins()
}

private fun AbstractKotlinNativeCompilation.addCompilerPlugins() {
    val project = target.project

    project.launchInStage(KotlinPluginLifecycle.Stage.AfterEvaluateBuildscript) {
        SubpluginEnvironment
            .loadSubplugins(project)
            .addSubpluginOptions(project, this@addCompilerPlugins)

        compileTaskProvider.configure {
            it.compilerPluginClasspath = this@addCompilerPlugins.configurations.pluginConfiguration
        }
    }
}
