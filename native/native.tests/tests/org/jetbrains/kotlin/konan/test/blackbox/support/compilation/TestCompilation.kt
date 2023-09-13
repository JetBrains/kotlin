/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.compilation

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.konan.properties.resolvablePropertyList
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase.*
import org.jetbrains.kotlin.konan.test.blackbox.support.TestModule.Companion.allDependsOn
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.ExecutableCompilation.Companion.applyPartialLinkageArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.ExecutableCompilation.Companion.applyTestRunnerSpecificArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.ExecutableCompilation.Companion.assertTestDumpFileNotEmptyIfExists
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationDependencyType.*
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.*
import org.jetbrains.kotlin.konan.test.blackbox.support.util.ArgsBuilder
import org.jetbrains.kotlin.konan.test.blackbox.support.util.buildArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.util.flatMapToSet
import org.jetbrains.kotlin.konan.test.blackbox.support.util.mapToSet
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import java.io.File

internal abstract class TestCompilation<A : TestCompilationArtifact> {
    abstract val result: TestCompilationResult<out A>
}

internal abstract class BasicCompilation<A : TestCompilationArtifact>(
    protected val targets: KotlinNativeTargets,
    protected val home: KotlinNativeHome,
    private val classLoader: KotlinNativeClassLoader,
    private val optimizationMode: OptimizationMode,
    private val compilerOutputInterceptor: CompilerOutputInterceptor,
    protected val freeCompilerArgs: TestCompilerArgs,
    protected val compilerPlugins: CompilerPlugins,
    protected val dependencies: CategorizedDependencies,
    protected val expectedArtifact: A
) : TestCompilation<A>() {
    protected abstract val sourceModules: Collection<TestModule>
    protected abstract val binaryOptions: Map<String, String>

    // Runs the compiler and memorizes the result on property access.
    final override val result: TestCompilationResult<out A> by lazy {
        val failures = dependencies.failures
        if (failures.isNotEmpty())
            TestCompilationResult.DependencyFailures(causes = failures)
        else
            doCompile()
    }

    private fun ArgsBuilder.applyCommonArgs() {
        add("-target", targets.testTarget.name)
        optimizationMode.compilerFlag?.let { compilerFlag -> add(compilerFlag) }
        add(
            "-enable-assertions",
            "-Xverify-ir=error"
        )
        addFlattened(binaryOptions.entries) { (name, value) -> listOf("-Xbinary=$name=$value") }
    }

    protected abstract fun applySpecificArgs(argsBuilder: ArgsBuilder)
    protected abstract fun applyDependencies(argsBuilder: ArgsBuilder)

    private fun ArgsBuilder.applyFreeArgs() {
        add(freeCompilerArgs.compilerArgs)
    }

    private fun ArgsBuilder.applyCompilerPlugins() {
        add(compilerPlugins.compilerPluginJars) { compilerPluginJar -> "-Xplugin=${compilerPluginJar.path}" }
    }

    private fun ArgsBuilder.applySources() {
        addFlattenedTwice(sourceModules, { it.files }) { it.location.path }
    }

    protected open fun postCompileCheck() = Unit

    private fun doCompile(): TestCompilationResult.ImmediateResult<out A> {
        val compilerArgs = buildArgs {
            applyCommonArgs()
            applySpecificArgs(this)
            applyDependencies(this)
            applyFreeArgs()
            applyCompilerPlugins()
            applySources()
        }

        val loggedCompilerParameters = LoggedData.CompilerParameters(home, compilerArgs, sourceModules)

        val (loggedCompilerCall: LoggedData, result: TestCompilationResult.ImmediateResult<out A>) = try {
            val compilerToolCallResult = when (compilerOutputInterceptor) {
                CompilerOutputInterceptor.DEFAULT -> callCompiler(
                    compilerArgs = compilerArgs,
                    kotlinNativeClassLoader = classLoader.classLoader
                )
                CompilerOutputInterceptor.NONE -> callCompilerWithoutOutputInterceptor(
                    compilerArgs = compilerArgs,
                    kotlinNativeClassLoader = classLoader.classLoader
                )
            }

            val (exitCode, compilerOutput, compilerOutputHasErrors, duration) = compilerToolCallResult

            val loggedCompilationToolCall =
                LoggedData.CompilationToolCall("COMPILER", loggedCompilerParameters, exitCode, compilerOutput, compilerOutputHasErrors, duration)

            val result = if (exitCode != ExitCode.OK || compilerOutputHasErrors)
                TestCompilationResult.CompilationToolFailure(loggedCompilationToolCall)
            else
                TestCompilationResult.Success(expectedArtifact, loggedCompilationToolCall)

            loggedCompilationToolCall to result
        } catch (unexpectedThrowable: Throwable) {
            val loggedFailure = LoggedData.CompilationToolCallUnexpectedFailure(loggedCompilerParameters, unexpectedThrowable)
            val result = TestCompilationResult.UnexpectedFailure(loggedFailure)

            loggedFailure to result
        }

        expectedArtifact.logFile.writeText(loggedCompilerCall.toString())

        postCompileCheck()

        return result
    }
}

internal abstract class SourceBasedCompilation<A : TestCompilationArtifact>(
    targets: KotlinNativeTargets,
    home: KotlinNativeHome,
    classLoader: KotlinNativeClassLoader,
    optimizationMode: OptimizationMode,
    compilerOutputInterceptor: CompilerOutputInterceptor,
    private val threadStateChecker: ThreadStateChecker,
    private val sanitizer: Sanitizer,
    private val gcType: GCType,
    private val gcScheduler: GCScheduler,
    private val allocator: Allocator,
    private val pipelineType: PipelineType,
    freeCompilerArgs: TestCompilerArgs,
    compilerPlugins: CompilerPlugins,
    override val sourceModules: Collection<TestModule>,
    dependencies: CategorizedDependencies,
    expectedArtifact: A
) : BasicCompilation<A>(
    targets = targets,
    home = home,
    classLoader = classLoader,
    optimizationMode = optimizationMode,
    compilerOutputInterceptor = compilerOutputInterceptor,
    freeCompilerArgs = freeCompilerArgs,
    compilerPlugins = compilerPlugins,
    dependencies = dependencies,
    expectedArtifact = expectedArtifact
) {
    override fun applySpecificArgs(argsBuilder: ArgsBuilder): Unit = with(argsBuilder) {
        add("-repo", home.librariesDir.path)
        threadStateChecker.compilerFlag?.let { compilerFlag -> add(compilerFlag) }
        sanitizer.compilerFlag?.let { compilerFlag -> add(compilerFlag) }
        gcType.compilerFlag?.let { compilerFlag -> add(compilerFlag) }
        gcScheduler.compilerFlag?.let { compilerFlag -> add(compilerFlag) }
        pipelineType.compilerFlags.forEach { compilerFlag -> add(compilerFlag) }
        applyK2MPPArgs(this)
    }

    override fun applyDependencies(argsBuilder: ArgsBuilder): Unit = with(argsBuilder) {
        addFlattened(dependencies.libraries) { library -> listOf("-l", library.path) }
        dependencies.friends.takeIf(Collection<*>::isNotEmpty)?.let { friends ->
            add("-friend-modules", friends.joinToString(File.pathSeparator) { friend -> friend.path })
        }
        add(dependencies.includedLibraries) { include -> "-Xinclude=${include.path}" }
    }

    private fun applyK2MPPArgs(argsBuilder: ArgsBuilder): Unit = with(argsBuilder) {
        if (pipelineType == PipelineType.K2 && freeCompilerArgs.compilerArgs.any { it == "-XXLanguage:+MultiPlatformProjects" }) {
            sourceModules.mapToSet { "-Xfragments=${it.name}" }
                .sorted().forEach { add(it) }
            sourceModules.flatMapToSet { module -> module.allDependsOn.map { "-Xfragment-refines=${module.name}:${it.name}" } }
                .sorted().forEach { add(it) }
            sourceModules.flatMapToSet { module -> module.files.map { "-Xfragment-sources=${module.name}:${it.location.path}" } }
                .sorted().forEach { add(it) }
        }
    }
}

internal class LibraryCompilation(
    settings: Settings,
    freeCompilerArgs: TestCompilerArgs,
    sourceModules: Collection<TestModule>,
    dependencies: Iterable<TestCompilationDependency<*>>,
    expectedArtifact: KLIB
) : SourceBasedCompilation<KLIB>(
    targets = settings.get(),
    home = settings.get(),
    classLoader = settings.get(),
    optimizationMode = settings.get(),
    compilerOutputInterceptor = settings.get(),
    threadStateChecker = settings.get(),
    sanitizer = settings.get(),
    gcType = settings.get(),
    gcScheduler = settings.get(),
    allocator = settings.get(),
    pipelineType = settings.get(),
    freeCompilerArgs = freeCompilerArgs,
    compilerPlugins = settings.get(),
    sourceModules = sourceModules,
    dependencies = CategorizedDependencies(dependencies),
    expectedArtifact = expectedArtifact
) {
    override val binaryOptions get() = BinaryOptions.RuntimeAssertionsMode.defaultForTesting

    override fun applySpecificArgs(argsBuilder: ArgsBuilder) = with(argsBuilder) {
        add(
            "-produce", "library",
            "-output", expectedArtifact.path
        )
        super.applySpecificArgs(argsBuilder)
    }
}

internal class ObjCFrameworkCompilation(
    settings: Settings,
    freeCompilerArgs: TestCompilerArgs,
    sourceModules: Collection<TestModule>,
    dependencies: Iterable<TestCompilationDependency<*>>,
    expectedArtifact: ObjCFramework
) : SourceBasedCompilation<ObjCFramework>(
    targets = settings.get(),
    home = settings.get(),
    classLoader = settings.get(),
    optimizationMode = settings.get(),
    compilerOutputInterceptor = settings.get(),
    threadStateChecker = settings.get(),
    sanitizer = settings.get(),
    gcType = settings.get(),
    gcScheduler = settings.get(),
    allocator = settings.get(),
    pipelineType = settings.getStageDependentPipelineType(),
    freeCompilerArgs = freeCompilerArgs,
    compilerPlugins = settings.get(),
    sourceModules = sourceModules,
    dependencies = CategorizedDependencies(dependencies),
    expectedArtifact = expectedArtifact
) {
    override val binaryOptions get() = BinaryOptions.RuntimeAssertionsMode.defaultForTesting

    override fun applySpecificArgs(argsBuilder: ArgsBuilder) = with(argsBuilder) {
        add(
            "-produce", "framework",
            "-output", expectedArtifact.frameworkDir.absolutePath
        )
        super.applySpecificArgs(argsBuilder)
    }
}

internal class GivenLibraryCompilation(givenArtifact: KLIB) : TestCompilation<KLIB>() {
    override val result = TestCompilationResult.Success(givenArtifact, LoggedData.NoopCompilerCall(givenArtifact.klibFile))
}

internal class CInteropCompilation(
    targets: KotlinNativeTargets,
    classLoader: KotlinNativeClassLoader,
    freeCompilerArgs: TestCompilerArgs,
    defFile: File,
    expectedArtifact: KLIB
) : TestCompilation<KLIB>() {

    override val result: TestCompilationResult<out KLIB> by lazy {
        val extraArgsArray = freeCompilerArgs.compilerArgs.toTypedArray()
        val loggedCInteropParameters = LoggedData.CInteropParameters(extraArgs = extraArgsArray, defFile = defFile)
        val (loggedCall: LoggedData, immediateResult: TestCompilationResult.ImmediateResult<out KLIB>) = try {
            val (exitCode, cinteropOutput, cinteropOutputHasErrors, duration) = invokeCInterop(
                classLoader.classLoader,
                targets,
                defFile,
                expectedArtifact.klibFile,
                extraArgsArray
            )

            val loggedInteropCall = LoggedData.CompilationToolCall(
                toolName = "CINTEROP",
                parameters = loggedCInteropParameters,
                exitCode = exitCode,
                toolOutput = cinteropOutput,
                toolOutputHasErrors = cinteropOutputHasErrors,
                duration = duration
            )
            val res = if (exitCode != ExitCode.OK || cinteropOutputHasErrors)
                TestCompilationResult.CompilationToolFailure(loggedInteropCall)
            else
                TestCompilationResult.Success(expectedArtifact, loggedInteropCall)

            loggedInteropCall to res
        } catch (unexpectedThrowable: Throwable) {
            val loggedFailure = LoggedData.CompilationToolCallUnexpectedFailure(loggedCInteropParameters, unexpectedThrowable)
            val res = TestCompilationResult.UnexpectedFailure(loggedFailure)

            loggedFailure to res
        }
        expectedArtifact.logFile.writeText(loggedCall.toString())

        immediateResult
    }
}

internal class ExecutableCompilation(
    settings: Settings,
    freeCompilerArgs: TestCompilerArgs,
    sourceModules: Collection<TestModule>,
    private val extras: Extras,
    dependencies: Iterable<TestCompilationDependency<*>>,
    expectedArtifact: Executable,
    val tryPassSystemCacheDirectory: Boolean = true,
) : SourceBasedCompilation<Executable>(
    targets = settings.get(),
    home = settings.get(),
    classLoader = settings.get(),
    optimizationMode = settings.get(),
    compilerOutputInterceptor = settings.get(),
    threadStateChecker = settings.get(),
    sanitizer = settings.get(),
    gcType = settings.get(),
    gcScheduler = settings.get(),
    allocator = settings.get(),
    pipelineType = settings.getStageDependentPipelineType(),
    freeCompilerArgs = freeCompilerArgs,
    compilerPlugins = settings.get(),
    sourceModules = sourceModules,
    dependencies = CategorizedDependencies(dependencies),
    expectedArtifact = expectedArtifact
) {
    private val cacheMode: CacheMode = settings.get()
    override val binaryOptions = BinaryOptions.RuntimeAssertionsMode.chooseFor(cacheMode)

    private val partialLinkageConfig: UsedPartialLinkageConfig = settings.get()

    override fun applySpecificArgs(argsBuilder: ArgsBuilder): Unit = with(argsBuilder) {
        add(
            "-produce", "program",
            "-output", expectedArtifact.path
        )
        when (extras) {
            is NoTestRunnerExtras -> add("-entry", extras.entryPoint)
            is WithTestRunnerExtras -> {
                val testDumpFile: File? = if (sourceModules.isEmpty()
                    && dependencies.includedLibraries.isNotEmpty()
                    && cacheMode.useStaticCacheForUserLibraries
                ) {
                    // If there are no source modules passed to the compiler, but there is an included library with the static cache, then
                    // this should be two-stage test mode: Test functions are already stored in the included library, and they should
                    // already have been dumped during generation of library's static cache.
                    null // No, don't need to dump tests.
                } else {
                    expectedArtifact.testDumpFile // Yes, need to dump tests.

                }

                applyTestRunnerSpecificArgs(extras, testDumpFile)
            }
        }
        applyPartialLinkageArgs(partialLinkageConfig)
        super.applySpecificArgs(argsBuilder)
    }

    override fun applyDependencies(argsBuilder: ArgsBuilder): Unit = with(argsBuilder) {
        super.applyDependencies(argsBuilder)
        cacheMode.staticCacheForDistributionLibrariesRootDir
            ?.takeIf { tryPassSystemCacheDirectory }
            ?.let { cacheRootDir -> add("-Xcache-directory=$cacheRootDir") }
        add(dependencies.uniqueCacheDirs) { libraryCacheDir -> "-Xcache-directory=${libraryCacheDir.path}" }
    }

    override fun postCompileCheck() {
        expectedArtifact.assertTestDumpFileNotEmptyIfExists()
    }

    companion object {
        internal fun ArgsBuilder.applyTestRunnerSpecificArgs(extras: WithTestRunnerExtras, testDumpFile: File?) {
            val testRunnerArg = when (extras.runnerType) {
                TestRunnerType.DEFAULT -> "-generate-test-runner"
                TestRunnerType.WORKER -> "-generate-worker-test-runner"
                TestRunnerType.NO_EXIT -> "-generate-no-exit-test-runner"
            }
            add(testRunnerArg)
            testDumpFile?.let { add("-Xdump-tests-to=$it") }
        }

        internal fun Executable.assertTestDumpFileNotEmptyIfExists() {
            if (testDumpFile.exists()) {
                testDumpFile.useLines { lines ->
                    assertTrue(lines.filter(String::isNotBlank).any()) { "Test dump file is empty: $testDumpFile" }
                }
            }
        }

        internal fun ArgsBuilder.applyPartialLinkageArgs(partialLinkageConfig: UsedPartialLinkageConfig) {
            with(partialLinkageConfig.config) {
                add("-Xpartial-linkage=${mode.name.lowercase()}")
                if (mode.isEnabled)
                    add("-Xpartial-linkage-loglevel=${logLevel.name.lowercase()}")
            }
        }
    }
}

internal class StaticCacheCompilation(
    settings: Settings,
    freeCompilerArgs: TestCompilerArgs,
    private val options: Options,
    private val pipelineType: PipelineType,
    dependencies: Iterable<TestCompilationDependency<*>>,
    expectedArtifact: KLIBStaticCache
) : BasicCompilation<KLIBStaticCache>(
    targets = settings.get(),
    home = settings.get(),
    classLoader = settings.get(),
    optimizationMode = settings.get(),
    compilerOutputInterceptor = settings.get(),
    freeCompilerArgs = freeCompilerArgs,
    compilerPlugins = settings.get(),
    dependencies = CategorizedDependencies(dependencies),
    expectedArtifact = expectedArtifact
) {
    sealed interface Options {
        object Regular : Options
        class ForIncludedLibraryWithTests(val expectedExecutableArtifact: Executable, val extras: WithTestRunnerExtras) : Options
    }

    override val sourceModules get() = emptyList<TestModule>()
    override val binaryOptions get() = BinaryOptions.RuntimeAssertionsMode.forUseWithCache

    private val cacheRootDir: File = run {
        val cacheMode = settings.get<CacheMode>()
        cacheMode.staticCacheForDistributionLibrariesRootDir ?: fail { "No cache root directory found for cache mode $cacheMode" }
    }

    private val makePerFileCache: Boolean = settings.get<CacheMode>().makePerFileCaches

    private val partialLinkageConfig: UsedPartialLinkageConfig = settings.get()

    override fun applySpecificArgs(argsBuilder: ArgsBuilder): Unit = with(argsBuilder) {
        add("-produce", "static_cache")
        pipelineType.compilerFlags.forEach { compilerFlag -> add(compilerFlag) }

        when (options) {
            is Options.Regular -> Unit /* Nothing to do. */
            is Options.ForIncludedLibraryWithTests -> {
                applyTestRunnerSpecificArgs(options.extras, options.expectedExecutableArtifact.testDumpFile)
                add("-Xinclude=${dependencies.libraryToCache.path}")
            }
        }

        // The following line adds "-Xembed-bitcode-marker" for certain iOS device targets:
        add(home.properties.resolvablePropertyList("additionalCacheFlags", targets.testTarget.visibleName))
        add(
            "-Xadd-cache=${dependencies.libraryToCache.path}",
            "-Xcache-directory=${expectedArtifact.cacheDir.path}",
            "-Xcache-directory=$cacheRootDir"
        )
        if (makePerFileCache)
            add("-Xmake-per-file-cache")

        applyPartialLinkageArgs(partialLinkageConfig)
    }

    override fun applyDependencies(argsBuilder: ArgsBuilder): Unit = with(argsBuilder) {
        dependencies.friends.takeIf(Collection<*>::isNotEmpty)?.let { friends ->
            add("-friend-modules", friends.joinToString(File.pathSeparator) { friend -> friend.path })
        }
        addFlattened(dependencies.cachedLibraries) { (_, library) -> listOf("-l", library.path) }
        add(dependencies.uniqueCacheDirs) { libraryCacheDir -> "-Xcache-directory=${libraryCacheDir.path}" }
    }

    override fun postCompileCheck() {
        (options as? Options.ForIncludedLibraryWithTests)?.expectedExecutableArtifact?.assertTestDumpFileNotEmptyIfExists()
    }
}

internal class CategorizedDependencies(uncategorizedDependencies: Iterable<TestCompilationDependency<*>>) {
    val failures: Set<TestCompilationResult.Failure> by lazy {
        uncategorizedDependencies.flatMapToSet { dependency ->
            when (val result = (dependency as? TestCompilation<*>)?.result) {
                is TestCompilationResult.Failure -> listOf(result)
                is TestCompilationResult.DependencyFailures -> result.causes
                is TestCompilationResult.Success -> emptyList()
                null -> emptyList()
            }
        }
    }

    val libraries: List<KLIB> by lazy { uncategorizedDependencies.collectArtifacts<KLIB, Library>() }
    val friends: List<KLIB> by lazy { uncategorizedDependencies.collectArtifacts<KLIB, FriendLibrary>() }
    val includedLibraries: List<KLIB> by lazy { uncategorizedDependencies.collectArtifacts<KLIB, IncludedLibrary>() }

    val cachedLibraries: List<KLIBStaticCache> by lazy { uncategorizedDependencies.collectArtifacts<KLIBStaticCache, LibraryStaticCache>() }

    val libraryToCache: KLIB by lazy {
        val libraries: List<KLIB> = buildList {
            this += libraries
            this += includedLibraries
            if (isEmpty()) this += friends // Friends should be ignored if they come with the main library.
        }
        libraries.singleOrNull()
            ?: fail { "Only one library is expected as input for ${StaticCacheCompilation::class.java}, found: $libraries" }
    }

    val uniqueCacheDirs: Set<File> by lazy {
        cachedLibraries.mapToSet { (libraryCacheDir, _) -> libraryCacheDir } // Avoid repeating the same directory more than once.
    }

    private inline fun <reified A : TestCompilationArtifact, reified T : TestCompilationDependencyType<A>> Iterable<TestCompilationDependency<*>>.collectArtifacts(): List<A> {
        return mapNotNull { dependency -> if (dependency.type is T) dependency.artifact as A else null }
    }
}

private object BinaryOptions {
    object RuntimeAssertionsMode {
        // Here the 'default' is in the sense the default for testing, not the default for the compiler.
        val defaultForTesting: Map<String, String> = mapOf("runtimeAssertionsMode" to "panic")
        val forUseWithCache: Map<String, String> = mapOf("runtimeAssertionsMode" to "ignore")

        fun chooseFor(cacheMode: CacheMode) = if (cacheMode.useStaticCacheForDistributionLibraries) forUseWithCache else defaultForTesting
    }
}

internal fun Settings.getStageDependentPipelineType(): PipelineType =
    when (get<TestMode>()) {
        TestMode.ONE_STAGE_MULTI_MODULE -> get<PipelineType>()
        TestMode.TWO_STAGE_MULTI_MODULE -> PipelineType.K1  // Don't pass "-language_version 2.0" to the second stage
    }
