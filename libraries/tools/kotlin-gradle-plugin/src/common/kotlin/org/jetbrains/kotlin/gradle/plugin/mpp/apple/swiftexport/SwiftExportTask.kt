/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject


@DisableCachingByDefault(because = "Swift Export is experimental, so no caching for now")
internal abstract class SwiftExportTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val swiftExportClasspath: ConfigurableFileCollection

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    @get:Nested
    abstract val parameters: SwiftExportParameters

    @TaskAction
    fun run() {
        val workQueue = workerExecutor.classLoaderIsolation { workerSpec ->
            workerSpec.classpath.from(swiftExportClasspath)
        }

        workQueue.submit(SwiftExportAction::class.java) { workParameters ->
            workParameters.debugMode.set(parameters.debugMode)
            workParameters.swiftApiModuleName.set(parameters.swiftApiModuleName)
            workParameters.bridgeModuleName.set(parameters.bridgeModuleName)
            workParameters.konanDistribution.set(parameters.konanDistribution)
            workParameters.sourceRoot.set(parameters.sourceRoot)

            workParameters.swiftApiPath.set(parameters.swiftApiPath)
            workParameters.headerBridgePath.set(parameters.headerBridgePath)
            workParameters.kotlinBridgePath.set(parameters.kotlinBridgePath)
        }
    }
}