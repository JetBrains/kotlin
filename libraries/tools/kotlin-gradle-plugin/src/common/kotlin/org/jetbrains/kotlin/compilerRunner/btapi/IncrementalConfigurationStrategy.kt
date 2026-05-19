/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi

import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration.Companion.BACKUP_CLASSES
import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration.Companion.KEEP_IC_CACHES_IN_MEMORY
import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration.Companion.MODULE_BUILD_DIR
import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration.Companion.MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION
import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration.Companion.OUTPUT_DIRS
import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration.Companion.ROOT_PROJECT_DIR
import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration.Companion.UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.compilerRunner.IncrementalCompilationEnvironment
import java.nio.file.Path

internal interface IncrementalConfigurationStrategy<in T : BaseCompilationOperation.Builder> {
    fun configureIncrementalCompilationConfiguration(buildOperation: T)

    object Default : IncrementalConfigurationStrategy<BaseCompilationOperation.Builder> {
        override fun configureIncrementalCompilationConfiguration(buildOperation: BaseCompilationOperation.Builder) {}
    }
}

@OptIn(ExperimentalCompilerArgument::class)
internal fun BaseIncrementalCompilationConfiguration.Builder.setupBaseIncrementalConfiguration(
    icEnv: IncrementalCompilationEnvironment,
    outputDirs: Set<Path>,
) {
    this[ROOT_PROJECT_DIR] = icEnv.rootProjectDir.toPath()
    this[MODULE_BUILD_DIR] = icEnv.buildDir.toPath()
    this[BACKUP_CLASSES] = icEnv.icFeatures.preciseCompilationResultsBackup
    this[KEEP_IC_CACHES_IN_MEMORY] = icEnv.icFeatures.keepIncrementalCompilationCachesInMemory
    this[OUTPUT_DIRS] = outputDirs
    this[UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM] = icEnv.icFeatures.enableUnsafeIncrementalCompilationForMultiplatform
    this[MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION] = icEnv.icFeatures.enableMonotonousIncrementalCompileSetExpansion
}
