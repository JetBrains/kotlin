/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl

import org.jetbrains.kotlin.gradle.artifacts.createKlibArtifact
import org.jetbrains.kotlin.gradle.artifacts.klibOutputDirectory
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationInfo
import org.jetbrains.kotlin.gradle.plugin.KotlinNativeTargetConfigurator
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.enabledOnCurrentHostForBinariesCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.targets.native.internal.commonizeCInteropTask
import org.jetbrains.kotlin.gradle.targets.native.internal.copyCommonizeCInteropForIdeTask
import org.jetbrains.kotlin.gradle.targets.native.internal.createCInteropApiElementsKlibArtifact
import org.jetbrains.kotlin.gradle.targets.native.internal.locateOrCreateCInteropDependencyConfiguration
import org.jetbrains.kotlin.gradle.targets.native.internal.getOrRegisterDownloadKotlinNativeDistributionTask
import org.jetbrains.kotlin.gradle.targets.native.toolchain.chooseKotlinNativeProvider
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.newInstance

internal val KotlinCreateNativeCInteropTasksSideEffect = KotlinCompilationSideEffect<KotlinNativeCompilation> { compilation ->
    val project = compilation.project
    val compilationInfo = KotlinCompilationInfo(compilation)
    compilation.cinterops.all { interop ->
        val params = CInteropProcess.Params(
            settings = interop,
            targetName = compilation.target.name,
            compilationName = compilation.name,
            konanTarget = compilation.konanTarget,
            baseKlibName = run {
                val compilationPrefix = if (compilation.isMain()) project.name else compilation.name
                "$compilationPrefix-cinterop-${interop.name}"
            },
            services = project.objects.newInstance()
        )

        val interopTask = project.registerTask<CInteropProcess>(interop.interopProcessingTaskName, listOf(params)) {
            it.destinationDirectory.set(project.klibOutputDirectory(compilationInfo).dir("cinterop"))
            it.group = KotlinNativeTargetConfigurator.INTEROP_GROUP
            it.description = "Generates Kotlin/Native interop library '${interop.name}' " +
                    "for compilation '${compilation.compilationName}'" +
                    "of target '${it.konanTarget.name}'."
            val enabledOnCurrentHost = compilation.konanTarget.enabledOnCurrentHostForBinariesCompilation
            it.enabled = enabledOnCurrentHost
            it.definitionFile.set(params.settings.definitionFile)
            it.kotlinNativeProvider.set(it.chooseKotlinNativeProvider(enabledOnCurrentHost, it.konanTarget, project))
            it.isGeneratedCinterop = interop.isGeneratedCinterop

            it.kotlinCompilerArgumentsLogLevel
                .value(project.kotlinPropertiesProvider.kotlinCompilerArgumentsLogLevel)
                .finalizeValueOnRead()
            it.produceUnpackagedKlib.set(project.kotlinPropertiesProvider.useNonPackedKlibs)
            if (project.kotlinPropertiesProvider.useNonPackedKlibs) {
                it.outputs.dir(it.klibDirectory)
            } else {
                it.outputs.file(it.klibFile)
            }
        }

        project.launch {
            project.getOrRegisterDownloadKotlinNativeDistributionTask()
            project.commonizeCInteropTask()?.configure { commonizeCInteropTask ->
                commonizeCInteropTask.from(interopTask)
            }
            project.copyCommonizeCInteropForIdeTask()
        }

        val interopOutput = project.files(interopTask.map { it.outputFileProvider })
        with(compilation) {
            compileDependencyFiles += interopOutput
            // Accumulate this cinterop output so that KotlinNativeCompilationAssociator can forward
            // all main cinterop klibs to every associated compilation (test, swiftExportMain, etc.).
            cinteropOutputs.from(interopOutput)
            if (isMain()) {
                // Add interop library to special CInteropApiElements configuration
                createCInteropApiElementsKlibArtifact(compilation, interop, interopTask)

                // Add the interop library in publication.
                if (compilation.konanTarget.enabledOnCurrentHostForBinariesCompilation) {
                    createKlibArtifact(
                        compilation,
                        classifier = interop.classifier,
                        klibProducingTask = interopTask,
                    )
                }
            }
        }

        interop.dependencyFiles += project.locateOrCreateCInteropDependencyConfiguration(compilation)
    }
}
