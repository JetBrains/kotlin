/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.SwiftExportAction
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.SwiftExportTaskParameters
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.createFullyExportedSwiftExportedModule
import org.jetbrains.kotlin.gradle.targets.native.toolchain.KotlinNativeProvider
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.konan.target.Distribution
import javax.inject.Inject

@DisableCachingByDefault(because = "Swift Export is experimental, so no caching for now")
internal abstract class SwiftExportTask @Inject constructor(
    private val workerExecutor: WorkerExecutor,
    private val fileSystem: FileSystemOperations,
) : DefaultTask() {

    internal abstract class ModuleInput {
        @get:Input
        abstract val moduleName: Property<String>

        @get:Input
        @get:Optional
        abstract val flattenPackage: Property<String>

        @get:InputFiles
        @get:PathSensitive(PathSensitivity.RELATIVE)
        abstract val artifact: RegularFileProperty
    }

    @get:Nested
    abstract val mainModuleInput: ModuleInput

    @get:Nested
    abstract val kotlinNativeProvider: Property<KotlinNativeProvider>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val swiftExportClasspath: ConfigurableFileCollection

    @get:Nested
    abstract val parameters: SwiftExportTaskParameters

    @TaskAction
    fun run() {
        cleanup()

        // Run Swift Export with process isolation to avoid leakage for AA/IntelliJ classes. See KT-73438
        val swiftExportQueue = workerExecutor.processIsolation { workerSpec ->
            workerSpec.classpath.from(swiftExportClasspath)
        }

        val swiftModules = parameters.swiftModules.map {
            it.toMutableList().apply {
                add(
                    createFullyExportedSwiftExportedModule(
                        mainModuleInput.moduleName.get(),
                        mainModuleInput.flattenPackage.orNull,
                        mainModuleInput.artifact.getFile()
                    )
                )
            }
        }

        swiftExportQueue.submit(SwiftExportAction::class.java) { workParameters ->
            workParameters.bridgeModuleName.set(parameters.bridgeModuleName)
            workParameters.outputPath.set(parameters.outputPath)
            workParameters.stableDeclarationsOrder.set(parameters.stableDeclarationsOrder)
            workParameters.swiftModulesFile.set(parameters.swiftModulesFile)
            workParameters.swiftModules.set(swiftModules)
            workParameters.swiftExportSettings.set(parameters.swiftExportSettings)
            workParameters.konanDistribution.set(kotlinNativeProvider.flatMap { it.bundleDirectory }.map { Distribution(it) })
            workParameters.konanTarget.set(parameters.konanTarget)
        }
    }

    private fun cleanup() {
        fileSystem.delete {
            it.delete(parameters.outputPath)
        }
    }
}