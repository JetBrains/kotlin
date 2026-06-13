/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi.jvm

import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration.Companion.FORCE_RECOMPILATION
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration.Companion.ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration.Companion.PRECISE_JAVA_TRACKING
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration.Companion.USE_FIR_RUNNER
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation.Companion.INCREMENTAL_COMPILATION
import org.jetbrains.kotlin.compilerRunner.IncrementalCompilationEnvironment
import org.jetbrains.kotlin.compilerRunner.btapi.IncrementalConfigurationStrategy
import org.jetbrains.kotlin.compilerRunner.btapi.setupBaseIncrementalConfiguration
import org.jetbrains.kotlin.incremental.ClasspathChanges
import java.io.File
import java.nio.file.Path

internal class JvmIncrementalConfigurationStrategy(
    val icEnv: IncrementalCompilationEnvironment,
    val outputDirs: List<Path>,
) : IncrementalConfigurationStrategy<JvmCompilationOperation.Builder> {
    @OptIn(ExperimentalCompilerArgument::class)
    override fun configureIncrementalCompilationConfiguration(buildOperation: JvmCompilationOperation.Builder) {
        val classpathChanges = icEnv.classpathChanges
        if (classpathChanges is ClasspathChanges.ClasspathSnapshotEnabled) {
            @Suppress("DEPRECATION") val classpathSnapshotsOptions = buildOperation.snapshotBasedIcConfigurationBuilder(
                icEnv.workingDir.toPath(),
                icEnv.changedFiles,
                classpathChanges.classpathSnapshotFiles.currentClasspathEntrySnapshotFiles.map(File::toPath),
                classpathChanges.classpathSnapshotFiles.shrunkPreviousClasspathSnapshotFile.toPath(),
            ).apply {
                setupBaseIncrementalConfiguration(icEnv, outputDirs.toSet())
                this[FORCE_RECOMPILATION] = classpathChanges !is ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun
                this[PRECISE_JAVA_TRACKING] = icEnv.icFeatures.usePreciseJavaTracking
                this[USE_FIR_RUNNER] = icEnv.useJvmFirRunner

                when (classpathChanges) {
                    is ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.NoChanges -> {
                        this[ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES] = true
                    }
                    is ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableForNonIncrementalRun -> {
                        this[FORCE_RECOMPILATION] = true
                    }
                    else -> {}
                }
            }.build()

            buildOperation[INCREMENTAL_COMPILATION] = classpathSnapshotsOptions
        }
    }
}
