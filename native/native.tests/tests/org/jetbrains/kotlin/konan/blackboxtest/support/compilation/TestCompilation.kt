/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.compilation

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.konan.blackboxtest.support.*
import org.jetbrains.kotlin.konan.blackboxtest.support.TestCase.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilation.Companion.resultingArtifactPath
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.*
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.CacheKind.Companion.rootCacheDir
import org.jetbrains.kotlin.konan.blackboxtest.support.util.flatMapToSet
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import java.io.File

internal interface TestCompilation {
    val result: TestCompilationResult

    companion object {
        val TestCompilation.resultingArtifactPath: String
            get() = result.assertSuccess().resultingArtifact.path

        fun createForKlib(
            settings: Settings,
            freeCompilerArgs: TestCompilerArgs,
            sourceModules: Collection<TestModule>,
            dependencies: TestCompilationDependencies,
            expectedKlibFile: File
        ): TestCompilation = TestCompilationImpl.create(
            settings = settings,
            freeCompilerArgs = freeCompilerArgs,
            sourceModules = sourceModules,
            dependencies = dependencies,
            expectedArtifactFile = expectedKlibFile,
            specificCompilerArgs = TestCompilationImpl.compilerArgsForKlib()
        )

        fun createForExecutable(
            settings: Settings,
            freeCompilerArgs: TestCompilerArgs,
            sourceModules: Collection<TestModule>,
            extras: Extras,
            dependencies: TestCompilationDependencies,
            expectedExecutableFile: File
        ): TestCompilation = TestCompilationImpl.create(
            settings = settings,
            freeCompilerArgs = freeCompilerArgs,
            sourceModules = sourceModules,
            dependencies = dependencies,
            expectedArtifactFile = expectedExecutableFile,
            specificCompilerArgs = TestCompilationImpl.compilerArgsForExecutable(settings.get(), extras)
        )

        fun createForExistingArtifact(resultingArtifact: File): TestCompilation =
            object : TestCompilation {
                override val result = TestCompilationResult.ExistingArtifact(resultingArtifact)
            }
    }
}

internal sealed interface TestCompilationResult {
    sealed interface ImmediateResult : TestCompilationResult {
        val loggedData: LoggedData
    }

    sealed interface Success : TestCompilationResult {
        val resultingArtifact: File
    }

    sealed interface Failure : ImmediateResult

    data class ExistingArtifact(override val resultingArtifact: File) : Success
    data class CompiledArtifact(override val resultingArtifact: File, override val loggedData: LoggedData.CompilerCall) :
        ImmediateResult, Success

    data class CompilerFailure(override val loggedData: LoggedData.CompilerCall) : Failure
    data class UnexpectedFailure(override val loggedData: LoggedData.CompilerCallUnexpectedFailure) : Failure
    data class DependencyFailures(val causes: Set<Failure>) : TestCompilationResult

    companion object {
        fun TestCompilationResult.assertSuccess(): Success = when (this) {
            is Success -> this
            is Failure -> fail { describeFailure() }
            is DependencyFailures -> fail { describeDependencyFailures() }
        }

        fun TestCompilationResult.assertSuccessfullyCompiled(): CompiledArtifact = when (val result = assertSuccess()) {
            is CompiledArtifact -> result
            is ExistingArtifact -> fail { "Test compilation result contains pre-existing artifact: ${result.resultingArtifact}" }
        }

        private fun Failure.describeFailure() = loggedData.withErrorMessage(
            when (this@describeFailure) {
                is CompilerFailure -> "Compilation failed."
                is UnexpectedFailure -> "Compilation failed with unexpected exception."
            }
        )

        private fun DependencyFailures.describeDependencyFailures() =
            buildString {
                appendLine("Compilation aborted due to errors in dependency compilations (${causes.size} items). See details below.")
                appendLine()
                causes.forEachIndexed { index, cause ->
                    append("#").append(index + 1).append(". ")
                    appendLine(cause.describeFailure())
                }
            }
    }
}

/**
 * The dependencies of a particular [TestCompilation].
 *
 * [libraries] - the [TestCompilation]s (modules) that should yield KLIBs to be consumed as dependency libraries in the current compilation.
 * [friends] - similarly but friend modules (-friend-modules).
 * [includedLibraries] - similarly but included modules (-Xinclude).
 */
internal class TestCompilationDependencies(
    val libraries: Collection<TestCompilation> = emptyList(),
    val friends: Collection<TestCompilation> = emptyList(),
    val includedLibraries: Collection<TestCompilation> = emptyList()
) {
    fun collectFailures(): Set<TestCompilationResult.Failure> = listOf(libraries, friends, includedLibraries)
        .flatten()
        .flatMapToSet { compilation ->
            when (val result = compilation.result) {
                is TestCompilationResult.Failure -> listOf(result)
                is TestCompilationResult.DependencyFailures -> result.causes
                is TestCompilationResult.Success -> emptyList()
            }
        }
}

private class TestCompilationImpl(
    private val targets: KotlinNativeTargets,
    private val home: KotlinNativeHome,
    private val classLoader: KotlinNativeClassLoader,
    private val optimizationMode: OptimizationMode,
    private val memoryModel: MemoryModel,
    private val threadStateChecker: ThreadStateChecker,
    private val gcType: GCType,
    private val freeCompilerArgs: TestCompilerArgs,
    private val sourceModules: Collection<TestModule>,
    private val dependencies: TestCompilationDependencies,
    private val expectedArtifactFile: File,
    private val specificCompilerArgs: ArgsBuilder.() -> Unit
) : TestCompilation {
    // Runs the compiler and memorizes the result on property access.
    override val result: TestCompilationResult by lazy {
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
            "-output", expectedArtifactFile.path,
            "-Xskip-prerelease-check",
            "-Xverify-ir",
            "-Xbinary=runtimeAssertionsMode=panic"
        )
        optimizationMode.compilerFlag?.let { compilerFlag -> add(compilerFlag) }
        memoryModel.compilerFlags?.let { compilerFlags -> add(compilerFlags) }
        threadStateChecker.compilerFlag?.let { compilerFlag -> add(compilerFlag) }
        gcType.compilerFlag?.let { compilerFlag -> add(compilerFlag) }

        addFlattened(dependencies.libraries) { library -> listOf("-l", library.resultingArtifactPath) }
        dependencies.friends.takeIf(Collection<*>::isNotEmpty)?.let { friends ->
            add("-friend-modules", friends.joinToString(File.pathSeparator) { friend -> friend.resultingArtifactPath })
        }
        add(dependencies.includedLibraries) { include -> "-Xinclude=${include.resultingArtifactPath}" }
        add(freeCompilerArgs.compilerArgs)
    }

    private fun ArgsBuilder.applySources() {
        addFlattenedTwice(sourceModules, { it.files }) { it.location.path }
    }

    private fun doCompile(): TestCompilationResult.ImmediateResult {
        val compilerArgs = buildArgs {
            applyCommonArgs()
            specificCompilerArgs()
            applySources()
        }

        val loggedCompilerParameters = LoggedData.CompilerParameters(compilerArgs, sourceModules)

        val (loggedCompilerCall: LoggedData, result: TestCompilationResult.ImmediateResult) = try {
            val (exitCode, compilerOutput, compilerOutputHasErrors, duration) = callCompiler(
                compilerArgs = compilerArgs,
                kotlinNativeClassLoader = classLoader.classLoader
            )

            val loggedCompilerCall =
                LoggedData.CompilerCall(loggedCompilerParameters, exitCode, compilerOutput, compilerOutputHasErrors, duration)

            val result = if (exitCode != ExitCode.OK || compilerOutputHasErrors)
                TestCompilationResult.CompilerFailure(loggedCompilerCall)
            else
                TestCompilationResult.CompiledArtifact(expectedArtifactFile, loggedCompilerCall)

            loggedCompilerCall to result
        } catch (unexpectedThrowable: Throwable) {
            val loggedFailure = LoggedData.CompilerCallUnexpectedFailure(loggedCompilerParameters, unexpectedThrowable)
            val result = TestCompilationResult.UnexpectedFailure(loggedFailure)

            loggedFailure to result
        }

        getLogFile(expectedArtifactFile).writeText(loggedCompilerCall.toString())

        return result
    }

    companion object {
        fun compilerArgsForKlib(): ArgsBuilder.() -> Unit = { add("-produce", "library") }

        fun compilerArgsForExecutable(cacheKind: CacheKind, extras: Extras): ArgsBuilder.() -> Unit = {
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

        // Just a shortcut to reduce the number of arguments.
        fun create(
            settings: Settings,
            freeCompilerArgs: TestCompilerArgs,
            sourceModules: Collection<TestModule>,
            dependencies: TestCompilationDependencies,
            expectedArtifactFile: File,
            specificCompilerArgs: ArgsBuilder.() -> Unit
        ) = TestCompilationImpl(
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
            expectedArtifactFile = expectedArtifactFile,
            specificCompilerArgs = specificCompilerArgs
        )
    }
}

private class ArgsBuilder {
    private val args = mutableListOf<String>()

    fun add(vararg args: String) {
        this.args += args
    }

    fun add(args: Iterable<String>) {
        this.args += args
    }

    inline fun <T> add(rawArgs: Iterable<T>, transform: (T) -> String) {
        rawArgs.mapTo(args) { transform(it) }
    }

    inline fun <T> addFlattened(rawArgs: Iterable<T>, transform: (T) -> Iterable<String>) {
        rawArgs.flatMapTo(args) { transform(it) }
    }

    inline fun <T, R> addFlattenedTwice(rawArgs: Iterable<T>, transform1: (T) -> Iterable<R>, transform2: (R) -> String) {
        rawArgs.forEach { add(transform1(it), transform2) }
    }

    fun build(): Array<String> = args.toTypedArray()
}

private inline fun buildArgs(builderAction: ArgsBuilder.() -> Unit): Array<String> {
    return ArgsBuilder().apply(builderAction).build()
}

private fun getLogFile(expectedArtifactFile: File): File = expectedArtifactFile.resolveSibling(expectedArtifactFile.name + ".log")
