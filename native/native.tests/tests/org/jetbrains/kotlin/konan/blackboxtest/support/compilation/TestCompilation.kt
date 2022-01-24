/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.compilation

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.konan.blackboxtest.support.*
import org.jetbrains.kotlin.konan.blackboxtest.support.TestCase.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationArtifact.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationDependencyType.*
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.*
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.CacheKind.WithStaticCache
import org.jetbrains.kotlin.konan.blackboxtest.support.util.ArgsBuilder
import org.jetbrains.kotlin.konan.blackboxtest.support.util.buildArgs
import org.jetbrains.kotlin.konan.blackboxtest.support.util.flatMapToSet
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.io.File

internal abstract class TestCompilation<A : TestCompilationArtifact> {
    abstract val result: TestCompilationResult<out A>
}

internal abstract class BasicCompilation<A : TestCompilationArtifact>(
    private val targets: KotlinNativeTargets,
    private val classLoader: KotlinNativeClassLoader,
    private val optimizationMode: OptimizationMode,
    protected val dependencies: Iterable<TestCompilationDependency<*>>,
    protected val expectedArtifact: A
) : TestCompilation<A>() {
    protected abstract val sourceModules: Collection<TestModule>
    protected abstract val freeCompilerArgs: TestCompilerArgs

    // Runs the compiler and memorizes the result on property access.
    final override val result: TestCompilationResult<out A> by lazy {
        val failures = dependencies.collectFailures()
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
            "-Xskip-prerelease-check",
            "-Xverify-ir",
            "-Xbinary=runtimeAssertionsMode=panic"
        )
    }

    protected abstract fun applySpecificArgs(argsBuilder: ArgsBuilder)
    protected abstract fun applyDependencies(argsBuilder: ArgsBuilder)

    private fun ArgsBuilder.applyFreeArgs() {
        add(freeCompilerArgs.compilerArgs)
    }

    private fun ArgsBuilder.applySources() {
        addFlattenedTwice(sourceModules, { it.files }) { it.location.path }
    }

    protected open fun doBeforeCompile() = Unit

    private fun doCompile(): TestCompilationResult.ImmediateResult<out A> {
        doBeforeCompile()

        val compilerArgs = buildArgs {
            applyCommonArgs()
            applySpecificArgs(this)
            applyDependencies(this)
            applyFreeArgs()
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
        private fun Iterable<TestCompilationDependency<*>>.collectFailures() = flatMapToSet { dependency ->
            when (val result = (dependency as? TestCompilation<*>)?.result) {
                is TestCompilationResult.Failure -> listOf(result)
                is TestCompilationResult.DependencyFailures -> result.causes
                is TestCompilationResult.Success -> emptyList()
                null -> emptyList()
            }
        }

        @JvmStatic
        protected inline fun <reified A : TestCompilationArtifact, reified T : TestCompilationDependencyType<A>> Iterable<TestCompilationDependency<*>>.collectArtifacts(): List<A> {
            val concreteDependencyType = T::class.objectInstance
            val dependencyTypeMatcher: (TestCompilationDependencyType<*>) -> Boolean = if (concreteDependencyType != null) {
                { it == concreteDependencyType }
            } else {
                { it.canYield(A::class.java) }
            }

            return mapNotNull { dependency -> if (dependencyTypeMatcher(dependency.type)) dependency.artifact as A else null }
        }

        @JvmStatic
        protected val Iterable<TestCompilationDependency<*>>.cachedLibraries
            get() = collectArtifacts<KLIBStaticCache, LibraryStaticCache>()

        private fun getLogFile(expectedArtifactFile: File): File = expectedArtifactFile.resolveSibling(expectedArtifactFile.name + ".log")
    }
}

internal abstract class SourceBasedCompilation<A : TestCompilationArtifact>(
    targets: KotlinNativeTargets,
    private val home: KotlinNativeHome,
    classLoader: KotlinNativeClassLoader,
    optimizationMode: OptimizationMode,
    private val memoryModel: MemoryModel,
    private val threadStateChecker: ThreadStateChecker,
    private val gcType: GCType,
    override val freeCompilerArgs: TestCompilerArgs,
    override val sourceModules: Collection<TestModule>,
    dependencies: Iterable<TestCompilationDependency<*>>,
    expectedArtifact: A
) : BasicCompilation<A>(
    targets = targets,
    classLoader = classLoader,
    optimizationMode = optimizationMode,
    dependencies = dependencies,
    expectedArtifact = expectedArtifact
) {
    override fun applySpecificArgs(argsBuilder: ArgsBuilder): Unit = with(argsBuilder) {
        add(
            "-repo", home.dir.resolve("klib").path,
            "-output", expectedArtifact.path
        )
        memoryModel.compilerFlags?.let { compilerFlags -> add(compilerFlags) }
        threadStateChecker.compilerFlag?.let { compilerFlag -> add(compilerFlag) }
        gcType.compilerFlag?.let { compilerFlag -> add(compilerFlag) }
    }

    override fun applyDependencies(argsBuilder: ArgsBuilder): Unit = with(argsBuilder) {
        addFlattened(dependencies.libraries) { library -> listOf("-l", library.path) }
        dependencies.friends.takeIf(Collection<*>::isNotEmpty)?.let { friends ->
            add("-friend-modules", friends.joinToString(File.pathSeparator) { friend -> friend.path })
        }
        add(dependencies.includedLibraries) { include -> "-Xinclude=${include.path}" }
    }

    companion object {
        private val Iterable<TestCompilationDependency<*>>.libraries get() = collectArtifacts<KLIB, Library>()
        private val Iterable<TestCompilationDependency<*>>.friends get() = collectArtifacts<KLIB, FriendLibrary>()
        private val Iterable<TestCompilationDependency<*>>.includedLibraries get() = collectArtifacts<KLIB, IncludedLibrary>()
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
    memoryModel = settings.get(),
    threadStateChecker = settings.get(),
    gcType = settings.get(),
    freeCompilerArgs = freeCompilerArgs,
    sourceModules = sourceModules,
    dependencies = dependencies,
    expectedArtifact = expectedArtifact
) {
    override fun applySpecificArgs(argsBuilder: ArgsBuilder) = with(argsBuilder) {
        add("-produce", "library")
        super.applySpecificArgs(argsBuilder)
    }
}

internal class ExecutableCompilation(
    settings: Settings,
    freeCompilerArgs: TestCompilerArgs,
    sourceModules: Collection<TestModule>,
    private val extras: Extras,
    dependencies: Iterable<TestCompilationDependency<*>>,
    expectedArtifact: Executable
) : SourceBasedCompilation<Executable>(
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

    override fun applySpecificArgs(argsBuilder: ArgsBuilder): Unit = with(argsBuilder) {
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
        super.applySpecificArgs(argsBuilder)
    }

    override fun applyDependencies(argsBuilder: ArgsBuilder): Unit = with(argsBuilder) {
        super.applyDependencies(argsBuilder)
        cacheKind.safeAs<WithStaticCache>()?.rootCacheDir?.let { rootCacheDir -> add("-Xcache-directory=$rootCacheDir") }
        add(dependencies.cachedLibraries) { (libraryCacheDir, _) -> "-Xcache-directory=${libraryCacheDir.path}" }
    }
}

internal class StaticCacheCompilation(
    settings: Settings,
    dependencies: Iterable<TestCompilationDependency<*>>,
    expectedArtifact: KLIBStaticCache
) : BasicCompilation<KLIBStaticCache>(
    targets = settings.get(),
    classLoader = settings.get(),
    optimizationMode = settings.get(),
    dependencies = dependencies,
    expectedArtifact = expectedArtifact
) {
    override val sourceModules get() = emptyList<TestModule>()
    override val freeCompilerArgs get() = TestCompilerArgs.EMPTY

    private val cacheDir = expectedArtifact.file

    private val cacheKind: WithStaticCache = run {
        val cacheKind = settings.get<CacheKind>()
        cacheKind.safeAs<WithStaticCache>() ?: fail {
            "${WithStaticCache::class.java} is expected as the current cache kind in ${StaticCacheCompilation::class.java}, found: $cacheKind"
        }
    }

    override fun doBeforeCompile() {
        cacheDir.mkdirs() // Make sure the directory to hold the cache exists.
    }

    override fun applySpecificArgs(argsBuilder: ArgsBuilder): Unit = with(argsBuilder) {
        add("-produce", "static_cache")
        // TODO: additional cache flags: konanProperties.resolvablePropertyList("additionalCacheFlags", target.visibleName)
        add(
            "-Xadd-cache=${dependencies.libraryToCache.path}",
            "-Xcache-directory=${cacheDir.path}"
        )
        cacheKind.rootCacheDir?.let { rootCacheDir -> add("-Xcache-directory=$rootCacheDir") }
    }

    override fun applyDependencies(argsBuilder: ArgsBuilder): Unit = with(argsBuilder) {
        addFlattened(dependencies.cachedLibraries) { (libraryCacheDir, library) ->
            listOf(
                "-l", library.path,
                "-Xcache-directory=${libraryCacheDir.path}"
            )
        }
    }

    companion object {
        private val Iterable<TestCompilationDependency<*>>.libraryToCache: KLIB
            get() {
                val libraries = collectArtifacts<KLIB, TestCompilationDependencyType<KLIB>>()
                return libraries.singleOrNull() ?: fail {
                    "Only one library is expected as input for ${StaticCacheCompilation::class.java}, found: $libraries"
                }
            }
    }
}
