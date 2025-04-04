/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Exec
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.internal.attributes.setAttributeTo
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.attributes.KlibPackaging
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.statistics.NativeLinkTaskMetrics
import org.jetbrains.kotlin.gradle.targets.KotlinTargetSideEffect
import org.jetbrains.kotlin.gradle.targets.native.toolchain.chooseKotlinNativeProvider
import org.jetbrains.kotlin.gradle.tasks.ExternalDependenciesBuilder
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.konan.target.CompilerOutputKind

internal val KotlinNativeConfigureBinariesSideEffect = KotlinTargetSideEffect<KotlinNativeTarget> { target ->
    val project = target.project

    target.compilations.all {
        // Create configurations eagerly to prevent issues when a configuration created during task materialization
        // this can cause warnings during IDE import, because IDE "collects" configurations before task execution.
        it.resolvableApiConfiguration()
    }

    // Create link and run tasks.
    target.binaries.all {
        project.createLinkTask(it)
    }

    target.binaries.withType(Executable::class.java).all {
        project.createRunTask(it)
    }

    target.binaries.prefixGroups.all { prefixGroup ->
        val linkGroupTask = project.locateOrRegisterTask<Task>(prefixGroup.linkTaskName) {
            it.group = BasePlugin.BUILD_GROUP
            it.description = "Links all binaries for target '${target.name}'."
        }
        prefixGroup.binaries.all {
            linkGroupTask.dependsOn(it.linkTaskName)
        }
    }

    // Create an aggregate link task for each compilation.
    target.compilations.all {
        project.registerTask<DefaultTask>(it.binariesTaskName) { task ->
            task.group = BasePlugin.BUILD_GROUP
            task.description = "Links all binaries for compilation '${it.name}' of target '${it.target.name}'."
        }
    }

    project.launchInStage(KotlinPluginLifecycle.Stage.AfterEvaluateBuildscript) {
        target.binaries.forEach { binary ->
            project.tasks.named(binary.compilation.binariesTaskName).configure { binariesTask ->
                binariesTask.dependsOn(binary.linkTaskName)
            }
        }
    }

    /**
     * We create test binaries for all platforms but test runs only for:
     *  - host platforms: macosX64, linuxX64, mingwX64;
     *  - simulated platforms: iosX64, tvosX64, watchosX64.
     * See more in [KotlinNativeTargetWithTestsConfigurator] and its subclasses.
     */
    target.binaries.test(listOf(NativeBuildType.DEBUG)) { }
}

/**
 * Creates a resolvable configuration from non-resolvable "api" of [KotlinNativeCompilation]
 * Kotlin Native requires that only API dependencies can be exported. So we need to resolve API-only dependencies
 * and exported dependencies to check that.
 *
 * FIXME: KT-76704 consider removing this configuration and the validation that exported klibs are present in the api scope configuration
 */
internal fun KotlinNativeCompilation.resolvableApiConfiguration(): Configuration {
    val apiConfiguration = compilation.internal.configurations.apiConfiguration
    return project
        .configurations.maybeCreateResolvable(lowerCamelCaseName("resolvable", apiConfiguration.name)) {
            extendsFrom(apiConfiguration)
            val compileConfiguration = compilation.internal.configurations.compileDependencyConfiguration
            compileConfiguration.copyAttributesTo(project.providers, this)
            if (project.kotlinPropertiesProvider.useNonPackedKlibs) {
                KlibPackaging.setAttributeTo(project, attributes, false)
            }
        }
}

private fun Project.createLinkTask(binary: NativeBinary) {
    // workaround for too late compilation compilerOptions creation
    // which leads to not able run project.afterEvaluate because of wrong context
    // this afterEvaluate comes from NativeCompilerOptions
    @Suppress("DEPRECATION") val compilationCompilerOptions = binary.compilation.compilerOptions

    val linkTask = registerTask<KotlinNativeLink>(
        binary.linkTaskName, listOf(binary)
    ) { task ->
        val target = binary.target
        val compilation = binary.compilation

        task.group = BasePlugin.BUILD_GROUP
        task.description = "Links ${binary.outputKind.description} '${binary.name}' for a target '${target.name}'."
        task.dependsOn(compilation.compileTaskProvider)

        val enabledOnCurrentHost = binary.konanTarget.enabledOnCurrentHostForBinariesCompilation
        task.enabled = enabledOnCurrentHost
        task.toolOptions.freeCompilerArgs.value(compilationCompilerOptions.options.freeCompilerArgs)
        task.toolOptions.freeCompilerArgs.addAll(providers.provider { PropertiesProvider(project).nativeLinkArgs })
        task.runViaBuildToolsApi.value(false).disallowChanges() // K/N is not yet supported
        task.kotlinNativeProvider.set(task.chooseKotlinNativeProvider(enabledOnCurrentHost, task.konanTarget))

        // Frameworks actively uses symlinks.
        // Gradle build cache transforms symlinks into regular files https://guides.gradle.org/using-build-cache/#symbolic_links
        task.outputs.cacheIf { task.outputKind != CompilerOutputKind.FRAMEWORK }

        task.source(compilation.compileTaskProvider.flatMap { it.outputFile })
        task.includes.clear() // we need to include non '.kt' or '.kts' files
        task.disallowSourceChanges()

        task.apiFiles.from({ compilation.resolvableApiConfiguration().incoming.files })

        task.kotlinCompilerArgumentsLogLevel
            .value(project.kotlinPropertiesProvider.kotlinCompilerArgumentsLogLevel)
            .finalizeValueOnRead()

        val externalDependenciesBuilder = ExternalDependenciesBuilder(project, compilation)
        task.externalDependenciesBuildCompilerArgs
            .value(
                project.providers.provider {
                    externalDependenciesBuilder.buildCompilerArgs()
                }
            )
            .disallowChanges()
    }

    NativeLinkTaskMetrics.collectMetrics(this)

    if (binary !is TestExecutable) {
        tasks.named(binary.compilation.target.artifactsTaskName).dependsOn(linkTask)
        locateOrRegisterTask<Task>(LifecycleBasePlugin.ASSEMBLE_TASK_NAME).dependsOn(linkTask)
    }

    if (binary is Framework) {
        createFrameworkArtifact(binary, linkTask)
    }
}


private fun Project.createRunTask(binary: Executable) {
    val taskName = binary.runTaskName ?: return
    registerTask<Exec>(taskName) { exec ->
        exec.group = KotlinNativeTargetConfigurator.RUN_GROUP
        exec.description = "Executes Kotlin/Native executable ${binary.name} for target ${binary.target.name}"

        exec.enabled = binary.konanTarget.isCurrentHost

        exec.executable = binary.outputFile.absolutePath
        exec.workingDir = project.projectDir

        exec.onlyIf { binary.outputFile.exists() }
        exec.dependsOn(binary.linkTaskName)
    }
}
