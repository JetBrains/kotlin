/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl

import org.jetbrains.kotlin.gradle.artifacts.createKlibArtifact
import org.jetbrains.kotlin.gradle.artifacts.klibOutputDirectory
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationInfo
import org.jetbrains.kotlin.gradle.plugin.KotlinNativeTargetConfigurator
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.enabledOnCurrentHost
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.targets.native.internal.commonizeCInteropTask
import org.jetbrains.kotlin.gradle.targets.native.internal.copyCommonizeCInteropForIdeTask
import org.jetbrains.kotlin.gradle.targets.native.internal.createCInteropApiElementsKlibArtifact
import org.jetbrains.kotlin.gradle.targets.native.internal.locateOrCreateCInteropDependencyConfiguration
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
            it.destinationDir = project.klibOutputDirectory(compilationInfo).dir("cinterop").map { it.asFile }
            it.group = KotlinNativeTargetConfigurator.INTEROP_GROUP
            it.description = "Generates Kotlin/Native interop library '${interop.name}' " +
                    "for compilation '${compilation.compilationName}'" +
                    "of target '${it.konanTarget.name}'."
            it.enabled = compilation.konanTarget.enabledOnCurrentHost
            it.definitionFile.set(params.settings.definitionFile)
        }


        project.launch {
            project.commonizeCInteropTask()?.configure { commonizeCInteropTask ->
                commonizeCInteropTask.from(interopTask)
            }
            project.copyCommonizeCInteropForIdeTask()
        }

        val interopOutput = project.files(interopTask.map { it.outputFileProvider })
        with(compilation) {
            compileDependencyFiles += interopOutput
            if (isMain()) {
                // Add interop library to special CInteropApiElements configuration
                createCInteropApiElementsKlibArtifact(compilation.target, interop, interopTask)

                // Add the interop library in publication.
                createKlibArtifact(
                    compilation,
                    artifactFile = interopTask.flatMap { it.outputFileProvider },
                    classifier = "cinterop-${interop.name}",
                    producingTask = interopTask,
                )

                // We cannot add the interop library in an compilation output because in this case
                // IDE doesn't see this library in module dependencies. So we have to manually add
                // main interop libraries in dependencies of the default test compilation.
                target.compilations.findByName(KotlinCompilation.TEST_COMPILATION_NAME)?.let { testCompilation ->
                    testCompilation.compileDependencyFiles += interopOutput
                    testCompilation.cinterops.all {
                        it.dependencyFiles += interopOutput
                    }
                }
            }
        }

        interop.dependencyFiles += project.locateOrCreateCInteropDependencyConfiguration(compilation)
    }
}


