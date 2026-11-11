/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib.ic

import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.ICReporter
import org.jetbrains.kotlin.build.report.metrics.DoNothingBuildMetricsReporter
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.ic.IncrementalNativeCompilerRunner
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.incremental.AbstractIncrementalMultiModuleCompilerRunnerTest
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistoryJs
import org.jetbrains.kotlin.incremental.testingUtils.BuildLogFinder
import org.jetbrains.kotlin.konan.test.blackbox.support.NativeKlibICTestSupport
import org.jetbrains.kotlin.konan.test.blackbox.support.NativeMutliModuleKlibICTestSupport
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeHome
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.SimpleTestRunSettings
import org.jetbrains.kotlin.library.KlibConstants.KLIB_FILE_EXTENSION
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File

@ExtendWith(NativeMutliModuleKlibICTestSupport::class)
abstract class AbstractIncrementalNativeKlibMultiModuleCompilerRunnerTest :
    AbstractIncrementalMultiModuleCompilerRunnerTest<K2NativeCompilerArguments, ModulesApiHistoryJs>() {
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
        get() = super.buildLogFinder.copy(isFirEnabled = true, isKlibEnabled = true, isNativeEnabled = true)

    override fun makeForSingleModule(
        moduleCacheDir: File,
        sourceRoots: Iterable<File>,
        args: K2NativeCompilerArguments,
        moduleBuildHistoryFile: File,
        messageCollector: MessageCollector,
        reporter: ICReporter,
        modulesApiHistory: ModulesApiHistoryJs,
        providedChangedFiles: ChangedFiles?
    ) {
        val allKotlinFiles = sourceRoots.asSequence().flatMap { it.walk() }
            .filter { it.isFile && it.extension.equals("kt", ignoreCase = true) }.toList()

        val buildReporter = BuildReporter(icReporter = reporter, buildMetricsReporter = DoNothingBuildMetricsReporter)
        val compiler = IncrementalNativeCompilerRunner(
            moduleCacheDir, buildReporter,
            buildHistoryFile = buildHistoryFile(moduleCacheDir),
            modulesApiHistory = modulesApiHistory,
        )
        compiler.compile(allKotlinFiles, args, messageCollector, providedChangedFiles ?: ChangedFiles.DeterminableFiles.ToBeComputed)
    }

    override val modulesApiHistory: ModulesApiHistoryJs by lazy {
        ModulesApiHistoryJs(workingDir, incrementalModuleInfo)
    }

    override fun String.asOutputFileName(): String = klib
    override fun String.asArtifactFileName(): String = klib

    override fun transformToDependency(moduleName: String, rawArtifact: File): File {
        val dependencyFile = File(repository, moduleName.klib)
        rawArtifact.copyTo(dependencyFile)
        return dependencyFile
    }

    override fun K2NativeCompilerArguments.updateForSingleModule(moduleDependencies: List<String>, outFile: File) {
        val libs = buildList {
            add(testRunSettings.get<KotlinNativeHome>().stdlibFile.absolutePath)
            moduleDependencies.forEach {
                add(File(repository, it.klib).absolutePath)
            }
        }.toTypedArray()

        libraries = libs
        outputName = outFile.path
        moduleName = outFile.nameWithoutExtension
    }

    companion object {
        private val String.klib: String get() = "$this.$KLIB_FILE_EXTENSION"
    }
}
