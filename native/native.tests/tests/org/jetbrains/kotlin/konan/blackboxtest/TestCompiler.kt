/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.compilerRunner.processCompilerOutput
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.konan.blackboxtest.TestCompilation.Companion.resultingArtifactPath
import org.jetbrains.kotlin.konan.blackboxtest.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.blackboxtest.TestModule.Companion.allDependencies
import org.jetbrains.kotlin.konan.blackboxtest.TestModule.Companion.allFriends
import org.jetbrains.kotlin.konan.blackboxtest.util.*
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import java.io.*
import kotlin.system.measureTimeMillis

internal class TestCompilationFactory(private val environment: TestEnvironment) {
    private val cachedCompilations = ThreadSafeCache<TestCompilationCacheKey, TestCompilation>()

    private sealed interface TestCompilationCacheKey {
        data class Klib(val sourceModules: Set<TestModule>, val freeCompilerArgs: TestCompilerArgs) : TestCompilationCacheKey
        data class Executable(val sourceModules: Set<TestModule>) : TestCompilationCacheKey
    }

    fun testCasesToExecutable(testCases: Collection<TestCase>): TestCompilation {
        val rootModules = testCases.flatMapToSet { testCase -> testCase.rootModules }
        val cacheKey = TestCompilationCacheKey.Executable(rootModules)

        // Fast pass.
        cachedCompilations[cacheKey]?.let { return it }

        // Long pass.
        val freeCompilerArgs = rootModules.first().testCase.freeCompilerArgs
        val libraries = rootModules.flatMapToSet { it.allDependencies }.map { moduleToKlib(it, freeCompilerArgs) }
        val friends = rootModules.flatMapToSet { it.allFriends }.map { moduleToKlib(it, freeCompilerArgs) }

        return cachedCompilations.computeIfAbsent(cacheKey) {
            val entryPoint = testCases.singleOrNull()?.extras?.entryPoint

            TestCompilationImpl(
                environment = environment,
                freeCompilerArgs = freeCompilerArgs,
                sourceModules = rootModules,
                dependencies = TestCompilationDependencies(libraries = libraries, friends = friends),
                expectedArtifactFile = artifactFileForExecutable(rootModules),
                specificCompilerArgs = {
                    add("-produce", "program")
                    if (entryPoint != null) add("-entry", entryPoint) else add("-generate-test-runner")
                    environment.globalEnvironment.getRootCacheDirectory(debuggable = true)?.let { rootCacheDir ->
                        add("-Xcache-directory=$rootCacheDir")
                    }
                }
            )
        }
    }

    private fun moduleToKlib(sourceModule: TestModule, freeCompilerArgs: TestCompilerArgs): TestCompilation {
        val sourceModules = setOf(sourceModule)
        val cacheKey = TestCompilationCacheKey.Klib(sourceModules, freeCompilerArgs)

        // Fast pass.
        cachedCompilations[cacheKey]?.let { return it }

        // Long pass.
        val libraries = sourceModule.allDependencies.map { moduleToKlib(it, freeCompilerArgs) }
        val friends = sourceModule.allFriends.map { moduleToKlib(it, freeCompilerArgs) }

        return cachedCompilations.computeIfAbsent(cacheKey) {
            TestCompilationImpl(
                environment = environment,
                freeCompilerArgs = freeCompilerArgs,
                sourceModules = sourceModules,
                dependencies = TestCompilationDependencies(libraries = libraries, friends = friends),
                expectedArtifactFile = artifactFileForKlib(sourceModule, freeCompilerArgs),
                specificCompilerArgs = { add("-produce", "library") }
            )
        }
    }

    private fun artifactFileForExecutable(modules: Set<TestModule.Exclusive>) = when (modules.size) {
        1 -> artifactFileForExecutable(modules.first())
        else -> multiModuleArtifactFile(modules, environment.globalEnvironment.target.family.exeSuffix)
    }

    private fun artifactFileForExecutable(module: TestModule.Exclusive) =
        singleModuleArtifactFile(module, environment.globalEnvironment.target.family.exeSuffix)

    private fun artifactFileForKlib(module: TestModule, freeCompilerArgs: TestCompilerArgs) = when (module) {
        is TestModule.Exclusive -> singleModuleArtifactFile(module, "klib")
        is TestModule.Shared -> environment.sharedBinariesDir.resolve("${module.name}-${prettyHash(freeCompilerArgs.hashCode())}.klib")
    }

    private fun singleModuleArtifactFile(module: TestModule.Exclusive, extension: String): File {
        val artifactFileName = buildString {
            append(module.testCase.nominalPackageName.replace('.', '_')).append('.')
            if (extension == "klib") append(module.name).append('.')
            append(extension)
        }
        return artifactDirForPackageName(module.testCase.nominalPackageName).resolve(artifactFileName)
    }

    private fun multiModuleArtifactFile(modules: Collection<TestModule>, extension: String): File {
        var filesCount = 0
        var hash = 0
        val uniquePackageNames = hashSetOf<PackageName>()

        modules.forEach { module ->
            module.files.forEach { file ->
                filesCount++
                hash = hash * 31 + file.hashCode()
            }

            if (module is TestModule.Exclusive)
                uniquePackageNames += module.testCase.nominalPackageName
        }

        val commonPackageName = uniquePackageNames.findCommonPackageName()

        val artifactFileName = buildString {
            val prefix = filesCount.toString()
            repeat(4 - prefix.length) { append('0') }
            append(prefix).append('-')

            if (commonPackageName != null)
                append(commonPackageName.replace('.', '_')).append('-')

            append(prettyHash(hash))

            append('.').append(extension)
        }

        return artifactDirForPackageName(commonPackageName).resolve(artifactFileName)
    }

    private fun artifactDirForPackageName(packageName: PackageName?): File {
        val baseDir = environment.testBinariesDir
        val outputDir = if (packageName != null) baseDir.resolve(packageName.replace('.', '_')) else baseDir

        outputDir.mkdirs()

        return outputDir
    }
}

internal interface TestCompilation {
    val result: TestCompilationResult

    companion object {
        val TestCompilation.resultingArtifactPath: String
            get() = result.assertSuccess().resultingArtifact.path
    }
}

internal sealed interface TestCompilationResult {
    sealed interface ImmediateResult : TestCompilationResult {
        val loggedData: LoggedData
    }

    sealed interface Failure : ImmediateResult

    data class Success(val resultingArtifact: File, override val loggedData: LoggedData.CompilerCall) : ImmediateResult
    data class CompilerFailure(override val loggedData: LoggedData.CompilerCall) : Failure
    data class UnexpectedFailure(override val loggedData: LoggedData.CompilerCallUnexpectedFailure) : Failure
    data class DependencyFailures(val causes: Set<Failure>) : TestCompilationResult

    companion object {
        fun TestCompilationResult.assertSuccess(): Success = when (this) {
            is Success -> this
            is Failure -> fail { describeFailure() }
            is DependencyFailures -> fail { describeDependencyFailures() }
        }

        private fun Failure.describeFailure() = when (this) {
            is CompilerFailure -> "Compilation failed.\n\n$loggedData"
            is UnexpectedFailure -> "Compilation failed with unexpected exception.\n\n$loggedData"
        }

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
 * The dependencies of a particular [TestCompilationImpl].
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
    private val environment: TestEnvironment,
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
            "-g",
            "-target", environment.globalEnvironment.target.name,
            "-repo", environment.globalEnvironment.kotlinNativeHome.resolve("klib").path,
            "-output", expectedArtifactFile.path,
            "-Xskip-prerelease-check",
            "-Xverify-ir",
            "-Xbinary=runtimeAssertionsMode=panic"
        )

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
            val (exitCode, compilerOutput, compilerOutputHasErrors, durationMillis) = callCompiler(
                compilerArgs = compilerArgs,
                lazyKotlinNativeClassLoader = environment.globalEnvironment.lazyKotlinNativeClassLoader
            )

            val loggedCompilerCall =
                LoggedData.CompilerCall(loggedCompilerParameters, exitCode, compilerOutput, compilerOutputHasErrors, durationMillis)

            val result = if (exitCode != ExitCode.OK || compilerOutputHasErrors)
                TestCompilationResult.CompilerFailure(loggedCompilerCall)
            else
                TestCompilationResult.Success(expectedArtifactFile, loggedCompilerCall)

            loggedCompilerCall to result
        } catch (unexpectedThrowable: Throwable) {
            val loggedFailure = LoggedData.CompilerCallUnexpectedFailure(loggedCompilerParameters, unexpectedThrowable)
            val result = TestCompilationResult.UnexpectedFailure(loggedFailure)

            loggedFailure to result
        }

        getLogFile(expectedArtifactFile).writeText(loggedCompilerCall.toString())

        return result
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

private fun callCompiler(compilerArgs: Array<String>, lazyKotlinNativeClassLoader: Lazy<ClassLoader>): CompilerCallResult {
    val compilerXmlOutput: ByteArrayOutputStream
    val exitCode: ExitCode

    val durationMillis = measureTimeMillis {
        val kotlinNativeClassLoader by lazyKotlinNativeClassLoader

        val servicesClass = Class.forName(Services::class.java.canonicalName, true, kotlinNativeClassLoader)
        val emptyServices = servicesClass.getField("EMPTY").get(servicesClass)

        val compilerClass = Class.forName("org.jetbrains.kotlin.cli.bc.K2Native", true, kotlinNativeClassLoader)
        val entryPoint = compilerClass.getMethod(
            "execAndOutputXml",
            PrintStream::class.java,
            servicesClass,
            Array<String>::class.java
        )

        compilerXmlOutput = ByteArrayOutputStream()
        exitCode = PrintStream(compilerXmlOutput).use { printStream ->
            val result = entryPoint.invoke(compilerClass.getDeclaredConstructor().newInstance(), printStream, emptyServices, compilerArgs)
            ExitCode.valueOf(result.toString())
        }
    }

    val messageCollector: MessageCollector
    val compilerOutput: String

    ByteArrayOutputStream().use { outputStream ->
        PrintStream(outputStream).use { printStream ->
            messageCollector = GroupingMessageCollector(
                PrintingMessageCollector(printStream, MessageRenderer.SYSTEM_INDEPENDENT_RELATIVE_PATHS, true),
                false
            )
            processCompilerOutput(
                messageCollector,
                OutputItemsCollectorImpl(),
                compilerXmlOutput,
                exitCode
            )
            messageCollector.flush()
        }
        compilerOutput = outputStream.toString(Charsets.UTF_8.name())
    }

    return CompilerCallResult(exitCode, compilerOutput, messageCollector.hasErrors(), durationMillis)
}

private data class CompilerCallResult(
    val exitCode: ExitCode,
    val compilerOutput: String,
    val compilerOutputHasErrors: Boolean,
    val durationMillis: Long
)

private fun getLogFile(expectedArtifactFile: File): File = expectedArtifactFile.resolveSibling(expectedArtifactFile.name + ".log")
