/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.ic

import org.jetbrains.kotlin.build.report.*
import org.jetbrains.kotlin.build.report.metrics.BuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.BuildTimeMetric
import org.jetbrains.kotlin.cli.bc.K2Native
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.jvm.plugins.PluginsLoader
import org.jetbrains.kotlin.incremental.IncrementalCompilationContext
import org.jetbrains.kotlin.incremental.IncrementalCompilationFeatures
import org.jetbrains.kotlin.incremental.IncrementalKlibCachesManager
import org.jetbrains.kotlin.incremental.IncrementalKlibCompilerRunner
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistory
import java.io.File

class IncrementalNativeCompilerRunner(
        workingDir: File,
        reporter: BuildReporter<BuildTimeMetric, BuildPerformanceMetric>,
        buildHistoryFile: File?,
        modulesApiHistory: ModulesApiHistory,
        icFeatures: IncrementalCompilationFeatures = IncrementalCompilationFeatures.DEFAULT_CONFIGURATION,
        pluginsLoader: PluginsLoader? = null,
        lookupTrackerDelegate: LookupTracker = DO_NOTHING,
) : IncrementalKlibCompilerRunner<K2NativeCompilerArguments>(
        workingDir,
        "caches-native",
        reporter,
        buildHistoryFile,
        modulesApiHistory,
        icFeatures,
        pluginsLoader,
        lookupTrackerDelegate,
) {
    override fun createCacheManager(
            icContext: IncrementalCompilationContext, args: K2NativeCompilerArguments
    ): IncrementalKlibCachesManager {
        return IncrementalKlibCachesManager(icContext, cacheDirectory, Native)
    }

    override fun getCompiler(args: K2NativeCompilerArguments): K2Native {
        return K2Native()
    }

    override fun destinationDir(args: K2NativeCompilerArguments): File {
        return File(args.outputName!!)
    }

    override fun classpathForLibrarySetSnapshot(args: K2NativeCompilerArguments): List<File> =
            args.libraries.map { File(it) }

}
