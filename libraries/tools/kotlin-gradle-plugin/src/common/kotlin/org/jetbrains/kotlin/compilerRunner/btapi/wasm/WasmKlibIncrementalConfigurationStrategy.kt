/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi.wasm

import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.wasm.IncrementalModule
import org.jetbrains.kotlin.buildtools.api.wasm.WasmHistoryBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.wasm.operations.WasmKlibCompilationOperation
import org.jetbrains.kotlin.compilerRunner.IncrementalCompilationEnvironment
import org.jetbrains.kotlin.compilerRunner.btapi.IncrementalConfigurationStrategy
import org.jetbrains.kotlin.compilerRunner.btapi.setupBaseIncrementalConfiguration
import org.jetbrains.kotlin.incremental.IncrementalModuleInfo
import java.nio.file.Path

internal class WasmKlibIncrementalConfigurationStrategy(
    val icEnv: IncrementalCompilationEnvironment,
    val incrementalModuleInfo: IncrementalModuleInfo?,
    val outputDirs: List<Path>,
) :
    IncrementalConfigurationStrategy<WasmKlibCompilationOperation.Builder> {
    @OptIn(ExperimentalCompilerArgument::class)
    override fun configureIncrementalCompilationConfiguration(buildOperation: WasmKlibCompilationOperation.Builder) {
        buildOperation[WasmKlibCompilationOperation.INCREMENTAL_COMPILATION] =
            buildOperation.historyBasedIcConfigurationBuilder(
                icEnv.rootProjectDir.toPath(),
                icEnv.workingDir.toPath(),
                icEnv.changedFiles,
                incrementalModuleInfo?.let {
                    it.dirToModule.map { (dir, module) ->
                        IncrementalModule(
                            module.name,
                            dir.toPath(),
                            module.buildDir.toPath(),
                            module.buildHistoryFile.parentFile.toPath()
                        )
                    }
                } ?: emptyList()).apply {
                setupBaseIncrementalConfiguration(icEnv, outputDirs.toSet())
                this[WasmHistoryBasedIncrementalCompilationConfiguration.ROOT_PROJECT_BUILD_DIR] =
                    incrementalModuleInfo?.rootProjectBuildDir?.toPath()
                this[WasmHistoryBasedIncrementalCompilationConfiguration.HISTORY_FILE_DIR] =
                    icEnv.multiModuleICSettings.buildHistoryFile.parentFile.toPath()
            }.build()
    }
}
