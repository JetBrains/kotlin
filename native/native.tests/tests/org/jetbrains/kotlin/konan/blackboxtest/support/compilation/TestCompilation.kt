/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.compilation

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.konan.blackboxtest.support.*
import org.jetbrains.kotlin.konan.blackboxtest.support.TestCase.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationArtifact.Executable
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationArtifact.KLIB
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationDependencyType.*
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.*
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.CacheKind.Companion.rootCacheDir
import org.jetbrains.kotlin.konan.blackboxtest.support.util.ArgsBuilder
import org.jetbrains.kotlin.konan.blackboxtest.support.util.buildArgs
import org.jetbrains.kotlin.konan.blackboxtest.support.util.flatMapToSet
import java.io.File

internal abstract class TestCompilation<A : TestCompilationArtifact> {
    abstract val result: TestCompilationResult<out A>
}

internal abstract class BaseCompilation<A : TestCompilationArtifact>(
    private val targets: KotlinNativeTargets,
    private val home: KotlinNativeHome,
    private val classLoader: KotlinNativeClassLoader,
    private val optimizationMode: OptimizationMode,
    private val memoryModel: MemoryModel,
    private val threadStateChecker: ThreadStateChecker,
    private val gcType: GCType,
    private val freeCompilerArgs: TestCompilerArgs,
    private val sourceModules: Collection<TestModule>,
    private val dependencies: Collection<TestCompilationDependency<*>>,
    private val expectedArtifact: A
) : TestCompilation<A>() {
    // Runs the compiler and memorizes the result on property access.
    final override val result: TestCompilationResult<out A> by lazy {
        val failures = dependencies.collectFailures()
        if (failures.isNotEmpty())
            TestCompilationResult.DependencyFailures(causes = failures)
        else
            doCompile()
    }

    private fun ArgsBuilder.applyCommonArgs() {
        add(
            "-enable-assertions",
            "-target", targets.testTarget.name,
            "-repo", home.dir.resolve("klib").path,
            "-output", expectedArtifact.path,
            "-Xskip-prerelease-check",
            "-Xverify-ir",
            "-Xbinary=runtimeAssertionsMode=panic"
        )
        optimizationMode.compilerFlag?.let { compilerFlag -> add(compilerFlag) }
        memoryModel.compilerFlags?.let { compilerFlags -> add(compilerFlags) }
        threadStateChecker.compilerFlag?.let { compilerFlag -> add(compilerFlag) }
        gcType.compilerFlag?.let { compilerFlag -> add(compilerFlag) }

        addFlattened(dependencies.libraries) { library -> listOf("-l", library.path) }
        dependencies.friends.takeIf(Collection<*>::isNotEmpty)?.let { friends ->
            add("-friend-modules", friends.joinToString(File.pathSeparator) { friend -> friend.path })
        }
        add(dependencies.includedLibraries) { include -> "-Xinclude=${include.path}" }
        add(freeCompilerArgs.compilerArgs)
    }

    protected abstract fun ArgsBuilder.applySpecificCompilerArgs()

    private fun ArgsBuilder.applySources() {
        addFlattenedTwice(sourceModules, { it.files }) { it.location.path }
    }

    private fun doCompile(): TestCompilationResult.ImmediateResult<out A> {
        val compilerArgs = buildArgs {
            applyCommonArgs()
            applySpecificCompilerArgs()
            applySources()
        }

        val loggedCompilerParameters = LoggedData.CompilerParameters(compilerArgs, sourceModules)

        val (loggedCompilerCall: LoggedData, result: TestCompilationResult.ImmediateResult<out A>) = try {
            val (exitCode, compilerOutput, compilerOutputHasErrors, duration) = callCompiler(
                compilerArgs = compilerArgs,
                kotlinNativeClassLoader = classLoader.classLoader
            )

            val loggedCompilerCall =
                LoggedData.CompilerCall(loggedCompilerParameters, exitCode, compilerOutput, compilerOutputHasErrors, duration)

            val result = if (exitCode != ExitCode.OK || compilerOutputHasErrors)
                TestCompilationResult.CompilerFailure(loggedCompilerCall)
            else
                TestCompilationResult.Success(expectedArtifact, loggedCompilerCall)

            loggedCompilerCall to result
        } catch (unexpectedThrowable: Throwable) {
            val loggedFailure = LoggedData.CompilerCallUnexpectedFailure(loggedCompilerParameters, unexpectedThrowable)
            val result = TestCompilationResult.UnexpectedFailure(loggedFailure)

            loggedFailure to result
        }

        getLogFile(expectedArtifact.file).writeText(loggedCompilerCall.toString())

        return result
    }

    companion object {
        private fun Collection<TestCompilationDependency<*>>.collectFailures() = flatMapToSet { dependency ->
            when (val result = (dependency as? TestCompilation<*>)?.result) {
                is TestCompilationResult.Failure -> listOf(result)
                is TestCompilationResult.DependencyFailures -> result.causes
                is TestCompilationResult.Success -> emptyList()
                null -> emptyList()
            }
        }

        private inline fun <reified A : TestCompilationArtifact, reified T : TestCompilationDependencyType<A>> Collection<TestCompilationDependency<*>>.collectArtifacts() =
            mapNotNull { dependency -> if (dependency.type is T) dependency.artifact as A else null }

        private val Collection<TestCompilationDependency<*>>.libraries get() = collectArtifacts<KLIB, Library>()
        private val Collection<TestCompilationDependency<*>>.friends get() = collectArtifacts<KLIB, FriendLibrary>()
        private val Collection<TestCompilationDependency<*>>.includedLibraries get() = collectArtifacts<KLIB, IncludedLibrary>()

        private fun getLogFile(expectedArtifactFile: File): File = expectedArtifactFile.resolveSibling(expectedArtifactFile.name + ".log")
    }
}

internal class LibraryCompilation(
    settings: Settings,
    freeCompilerArgs: TestCompilerArgs,
    sourceModules: Collection<TestModule>,
    dependencies: Collection<TestCompilationDependency<*>>,
    expectedArtifact: KLIB
) : BaseCompilation<KLIB>(
    targets = settings.get(),
    home = settings.get(),
    classLoader = settings.get(),
    optimizationMode = settings.get(),
    memoryModel = settings.get(),
    threadStateChecker = settings.get(),
    gcType = settings.get(),
    freeCompilerArgs = freeCompilerArgs,
    sourceModules = sourceModules,
    dependencies = dependencies,
    expectedArtifact = expectedArtifact
) {
    override fun ArgsBuilder.applySpecificCompilerArgs() {
        add("-produce", "library")
    }
}

internal class ExecutableCompilation(
    settings: Settings,
    freeCompilerArgs: TestCompilerArgs,
    sourceModules: Collection<TestModule>,
    private val extras: Extras,
    dependencies: Collection<TestCompilationDependency<*>>,
    expectedArtifact: Executable
) : BaseCompilation<Executable>(
    targets = settings.get(),
    home = settings.get(),
    classLoader = settings.get(),
    optimizationMode = settings.get(),
    memoryModel = settings.get(),
    threadStateChecker = settings.get(),
    gcType = settings.get(),
    freeCompilerArgs = freeCompilerArgs,
    sourceModules = sourceModules,
    dependencies = dependencies,
    expectedArtifact = expectedArtifact
) {
    private val cacheKind: CacheKind = settings.get()

    override fun ArgsBuilder.applySpecificCompilerArgs() {
        add("-produce", "program")
        when (extras) {
            is NoTestRunnerExtras -> add("-entry", extras.entryPoint)
            is WithTestRunnerExtras -> {
                val testRunnerArg = when (extras.runnerType) {
                    TestRunnerType.DEFAULT -> "-generate-test-runner"
                    TestRunnerType.WORKER -> "-generate-worker-test-runner"
                    TestRunnerType.NO_EXIT -> "-generate-no-exit-test-runner"
                }
                add(testRunnerArg)
            }
        }
        cacheKind.rootCacheDir?.let { rootCacheDir -> add("-Xcache-directory=$rootCacheDir") }
    }
}
