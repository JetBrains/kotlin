/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib.ic

import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.metrics.DoNothingBuildMetricsReporter
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.incremental.AbstractIncrementalCompilerRunnerTestBase
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.cli.ic.IncrementalNativeCompilerRunner
import org.jetbrains.kotlin.incremental.multiproject.EmptyModulesApiHistory
import org.jetbrains.kotlin.incremental.testingUtils.BuildLogFinder
import org.jetbrains.kotlin.incremental.utils.TestCompilationResult
import org.jetbrains.kotlin.incremental.utils.TestICReporter
import org.jetbrains.kotlin.konan.test.blackbox.support.NativeKlibICTestSupport
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeHome
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.SimpleTestRunSettings
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File

@ExtendWith(NativeKlibICTestSupport::class)
abstract class AbstractIncrementalNativeKlibCompilerRunnerTest : AbstractIncrementalCompilerRunnerTestBase<K2NativeCompilerArguments>() {
    lateinit var testRunSettings: SimpleTestRunSettings

    override fun createCompilerArguments(destinationDir: File, testDir: File): K2NativeCompilerArguments =
        K2NativeCompilerArguments().apply {
            libraries = arrayOf(testRunSettings.get<KotlinNativeHome>().stdlibFile.absolutePath)
            outputName = destinationDir.path
            moduleName = testDir.name
            produce = "library"
            kotlinHome = testRunSettings.get<KotlinNativeHome>().dir.absolutePath
            languageVersion = LanguageVersion.LATEST_STABLE.versionString
        }

    override val buildLogFinder: BuildLogFinder
        get() = super.buildLogFinder.copy(
            isFirEnabled = true,
            isKlibEnabled = true,
            isNativeEnabled = true,
        )

    override fun make(cacheDir: File, outDir: File, sourceRoots: Iterable<File>, args: K2NativeCompilerArguments): TestCompilationResult {
        val reporter = TestICReporter()
        val messageCollector = MessageCollectorImpl()

        val allKotlinFiles = sourceRoots.asSequence().flatMap { it.walk() }
            .filter { it.isFile && it.extension.equals("kt", ignoreCase = true) }.toList()

        val buildReporter = BuildReporter(icReporter = reporter, buildMetricsReporter = DoNothingBuildMetricsReporter)
        val compiler = IncrementalNativeCompilerRunner(
            cacheDir, buildReporter,
            buildHistoryFile = buildHistoryFile(cacheDir),
            modulesApiHistory = EmptyModulesApiHistory,
        )
        compiler.compile(allKotlinFiles, args, messageCollector, ChangedFiles.DeterminableFiles.ToBeComputed)
        return TestCompilationResult(reporter, messageCollector)
    }

    override fun failFile(testDir: File): File = testDir.resolve("fail_native_k2.txt")
}
