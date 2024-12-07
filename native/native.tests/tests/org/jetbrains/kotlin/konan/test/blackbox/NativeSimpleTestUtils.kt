/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Binaries
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.PipelineType
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.util.DEFAULT_MODULE_NAME
import org.jetbrains.kotlin.konan.test.blackbox.support.util.LAUNCHER_MODULE_NAME
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.junit.jupiter.api.Assumptions
import java.io.File

internal abstract class ArtifactBuilder<T>(
    val test: AbstractNativeSimpleTest,
    val rootDir: File,
    val targetSrc: String,
    dependencies: List<TestCompilationDependency<*>>
) {
    private val buildDir = test.buildDir
    var outputDir: String = ""

    private val sources = mutableListOf<Pair<String, String>>()
    private val dependencies = dependencies.toMutableList()

    infix fun String.copyTo(to: String) {
        sources.add(Pair(this, to))
    }

    protected abstract fun build(sourcesDir: File, outputDir: File, dependencies: List<TestCompilationDependency<*>>): T

    fun build(): T {
        val targetSrc = buildDir.resolve(targetSrc)
        targetSrc.deleteRecursively()
        targetSrc.mkdirs()
        val outputDir = if (outputDir == "") buildDir else buildDir.resolve(outputDir)
        sources.forEach {
            val source = rootDir.resolve(it.first)
            val target = targetSrc.resolve(it.second)
            target.mkdirs()
            if (source.isFile)
                source.copyTo(target, true)
            else
                source.copyRecursively(target, true)
        }
        return build(targetSrc, outputDir, dependencies)
    }
}

internal class LibraryBuilder(
    test: AbstractNativeSimpleTest,
    rootDir: File,
    targetSrc: String,
    dependencies: List<TestCompilationDependency<*>>
) : ArtifactBuilder<TestCompilationArtifact.KLIB>(test, rootDir, targetSrc, dependencies) {
    override fun build(sourcesDir: File, outputDir: File, dependencies: List<TestCompilationDependency<*>>) =
        test.compileToLibrary(
            sourcesDir,
            outputDir,
            freeCompilerArgs = TestCompilerArgs.EMPTY,
            dependencies
        )
}

// WARNING: compiles in one-stage mode (sources->executable) even when `mode=TWO_STAGE_MULTI_MODULE`
internal class ExecutableBuilder(
    test: AbstractNativeSimpleTest,
    rootDir: File,
    targetSrc: String,
    val tryPassSystemCacheDirectory: Boolean,
    dependencies: List<TestCompilationDependency<*>>
) : ArtifactBuilder<CompiledExecutable>(test, rootDir, targetSrc, dependencies) {
    private val freeCompilerArgs = mutableListOf<String>()

    operator fun String.unaryPlus() {
        freeCompilerArgs.add(this)
    }

    // WARNING: compiles in one-stage mode (sources->executable) even when `mode=TWO_STAGE_MULTI_MODULE`
    override fun build(sourcesDir: File, outputDir: File, dependencies: List<TestCompilationDependency<*>>) =
        test.compileToExecutableInOneStage(
            sourcesDir,
            tryPassSystemCacheDirectory,
            freeCompilerArgs = if (freeCompilerArgs.isEmpty()) TestCompilerArgs.EMPTY else TestCompilerArgs(freeCompilerArgs),
            dependencies
        )
}

val AbstractNativeSimpleTest.buildDir: File get() = testRunSettings.get<Binaries>().testBinariesDir
val AbstractNativeSimpleTest.targets: KotlinNativeTargets get() = testRunSettings.get()

fun TestCompilationArtifact.KLIB.asLibraryDependency() =
    ExistingDependency(this, TestCompilationDependencyType.Library)

internal fun TestCompilationArtifact.KLIB.asIncludedLibraryDependency() =
    ExistingDependency(this, TestCompilationDependencyType.IncludedLibrary)

internal fun TestCompilationArtifact.KLIB.asFriendLibraryDependency() =
    ExistingDependency(this, TestCompilationDependencyType.FriendLibrary)

internal fun TestCompilationArtifact.KLIBStaticCache.asStaticCacheDependency() =
    ExistingDependency(this, TestCompilationDependencyType.LibraryStaticCache)

fun AbstractNativeSimpleTest.compileToLibrary(sourcesDir: File, vararg dependencies: TestCompilationArtifact.KLIB) =
    compileToLibrary(sourcesDir, buildDir, *dependencies)

fun AbstractNativeSimpleTest.compileToLibrary(testCase: TestCase, vararg dependencies: TestCompilationDependency<*>) =
    compileToLibrary(testCase, buildDir, dependencies.asList())

internal fun AbstractNativeSimpleTest.compileToLibrary(
    sourcesDir: File,
    outputDir: File,
    vararg dependencies: TestCompilationArtifact.KLIB
): TestCompilationArtifact.KLIB = compileToLibrary(sourcesDir, outputDir, TestCompilerArgs.EMPTY, dependencies.map { it.asLibraryDependency() })

internal fun AbstractNativeSimpleTest.compileToLibrary(
    sourcesDir: File,
    outputDir: File,
    freeCompilerArgs: TestCompilerArgs,
    dependencies: List<TestCompilationDependency<*>>
): TestCompilationArtifact.KLIB {
    val testCase: TestCase = generateTestCaseWithSingleModule(sourcesDir, freeCompilerArgs)
    val compilationResult = compileToLibrary(testCase, outputDir, dependencies)
    return compilationResult.resultingArtifact
}

fun AbstractNativeSimpleTest.cinteropToLibrary(
    targets: KotlinNativeTargets,
    defFile: File,
    outputDir: File,
    freeCompilerArgs: TestCompilerArgs
): TestCompilationResult<out TestCompilationArtifact.KLIB> {
    val args = freeCompilerArgs + cinteropModulesCachePathArguments(freeCompilerArgs.cinteropArgs, outputDir)
    val testCase: TestCase = generateCInteropTestCaseFromSingleDefFile(defFile, args)
    return CInteropCompilation(
        classLoader = testRunSettings.get(),
        targets = targets,
        freeCompilerArgs = args,
        defFile = testCase.modules.single().files.single().location,
        dependencies = emptyList(),
        expectedArtifact = getLibraryArtifact(testCase, outputDir)
    ).result
}

private fun cinteropModulesCachePathArguments(
    cinteropArgs: List<String>,
    outputDir: File,
) = if (cinteropArgs.contains("-fmodules") && cinteropArgs.none { it.startsWith(FMODULES_CACHE_PATH_EQ) }) {
    // Don't reuse the system-wide module cache to make the test run more predictably.
    // See e.g. https://youtrack.jetbrains.com/issue/KT-68254.
    TestCInteropArgs("-compiler-option", "$FMODULES_CACHE_PATH_EQ$outputDir/modulesCachePath")
} else {
    TestCompilerArgs.EMPTY
}

private const val FMODULES_CACHE_PATH_EQ = "-fmodules-cache-path="

internal class CompiledExecutable(
    val testCase: TestCase,
    val compilationResult: TestCompilationResult.Success<out TestCompilationArtifact.Executable>
) {
    val executableFile: File get() = compilationResult.resultingArtifact.executableFile

    val testExecutable by lazy { TestExecutable.fromCompilationResult(testCase, compilationResult) }
}

// WARNING: compiles in one-stage mode (sources->executable) even when `mode=TWO_STAGE_MULTI_MODULE`
internal fun AbstractNativeSimpleTest.compileToExecutableInOneStage(
    sourcesDir: File,
    tryPassSystemCacheDirectory: Boolean,
    freeCompilerArgs: TestCompilerArgs,
    vararg dependencies: TestCompilationArtifact.KLIB
) = compileToExecutableInOneStage(sourcesDir, tryPassSystemCacheDirectory, freeCompilerArgs, dependencies.map { it.asLibraryDependency() })

// WARNING: compiles in one-stage mode (sources->executable) even when `mode=TWO_STAGE_MULTI_MODULE`
internal fun AbstractNativeSimpleTest.compileToExecutableInOneStage(
    sourcesDir: File,
    tryPassSystemCacheDirectory: Boolean,
    freeCompilerArgs: TestCompilerArgs,
    dependencies: List<TestCompilationDependency<*>>
): CompiledExecutable {
    val testCase: TestCase = generateTestCaseWithSingleModule(sourcesDir, freeCompilerArgs)
    val compilationResult = compileToExecutableInOneStage(testCase, tryPassSystemCacheDirectory, dependencies)
    return CompiledExecutable(testCase, compilationResult.assertSuccess())
}

// WARNING: compiles in one-stage mode (sources->executable) even when `mode=TWO_STAGE_MULTI_MODULE`
fun AbstractNativeSimpleTest.compileToExecutableInOneStage(testCase: TestCase, vararg dependencies: TestCompilationDependency<*>) =
    compileToExecutableInOneStage(testCase, true, dependencies.asList())

internal fun AbstractNativeSimpleTest.compileToStaticCache(
    klib: TestCompilationArtifact.KLIB,
    cacheDir: File,
    vararg dependencies: TestCompilationArtifact.KLIBStaticCache
): TestCompilationArtifact.KLIBStaticCache {
    val compilation = StaticCacheCompilation(
        settings = testRunSettings,
        freeCompilerArgs = TestCompilerArgs.EMPTY,
        StaticCacheCompilation.Options.Regular,
        pipelineType = testRunSettings.get(),
        dependencies = buildList {
            this += klib.asLibraryDependency()
            dependencies.mapTo(this) { it.asStaticCacheDependency() }
        },
        expectedArtifact = TestCompilationArtifact.KLIBStaticCacheImpl(cacheDir, klib)
    )
    return compilation.result.assertSuccess().resultingArtifact
}

/**
 * [sourcesRoot] points either to a .kt-file, or a folder.
 *
 * If it's present, then it's name (without .kt-extension, if it's a file) will be used as 'moduleName' for generated module.
 */
fun AbstractNativeSimpleTest.generateTestCaseWithSingleModule(
    sourcesRoot: File?,
    freeCompilerArgs: TestCompilerArgs = TestCompilerArgs.EMPTY,
    extras: TestCase.Extras = TestCase.WithTestRunnerExtras(TestRunnerType.DEFAULT),
): TestCase {
    val moduleName: String = sourcesRoot?.name?.removeSuffix(".kt") ?: LAUNCHER_MODULE_NAME
    val module = TestModule.Exclusive(moduleName, emptySet(), emptySet(), emptySet())

    sourcesRoot?.walkTopDown()
        ?.filter { it.isFile && it.extension == "kt" }
        ?.forEach { file -> module.files += TestFile.createCommitted(file, module) }

    return TestCase(
        id = TestCaseId.Named(moduleName),
        kind = TestKind.STANDALONE,
        modules = setOf(module),
        freeCompilerArgs = freeCompilerArgs,
        nominalPackageName = PackageName.EMPTY,
        checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout),
        extras = extras
    ).apply {
        initialize(null, null)
    }
}

fun AbstractNativeSimpleTest.generateTestCaseWithSingleFile(
    sourceFile: File,
    moduleName: String = sourceFile.name,
    freeCompilerArgs: TestCompilerArgs = TestCompilerArgs.EMPTY,
    testKind: TestKind = TestKind.STANDALONE,
    extras: TestCase.Extras = TestCase.WithTestRunnerExtras(TestRunnerType.DEFAULT),
    checks: TestRunChecks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout),
): TestCase {
    val module = TestModule.Exclusive(moduleName, emptySet(), emptySet(), emptySet())
    module.files += TestFile.createCommitted(sourceFile, module)

    return TestCase(
        id = TestCaseId.Named(moduleName),
        kind = testKind,
        modules = setOf(module),
        freeCompilerArgs = freeCompilerArgs,
        nominalPackageName = PackageName.EMPTY,
        checks = checks,
        extras = extras
    ).apply {
        initialize(null, null)
    }
}

internal fun AbstractNativeSimpleTest.generateCInteropTestCaseFromSingleDefFile(
    defFile: File,
    freeCompilerArgs: TestCompilerArgs,
): TestCase {
    val moduleName: String = defFile.name
    val module = TestModule.Exclusive(moduleName, emptySet(), emptySet(), emptySet())
    module.files += TestFile.createCommitted(defFile, module)

    return TestCase(
        id = TestCaseId.Named(moduleName),
        kind = TestKind.STANDALONE,
        modules = setOf(module),
        freeCompilerArgs = freeCompilerArgs,
        nominalPackageName = PackageName.EMPTY,
        checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout),
        extras = TestCase.WithTestRunnerExtras(TestRunnerType.DEFAULT)
    ).apply {
        initialize(null, null)
    }
}

internal fun AbstractNativeSimpleTest.generateObjCFrameworkTestCase(
    kind: TestKind,
    extras: TestCase.Extras,
    moduleName: String,
    sources: List<File>,
    freeCompilerArgs: TestCompilerArgs = TestCompilerArgs.EMPTY,
    givenDependencies: Set<TestModule.Given>? = null,
    checks: TestRunChecks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout),
): TestCase {
    val module = TestModule.Exclusive(DEFAULT_MODULE_NAME, emptySet(), emptySet(), emptySet())
    sources.forEach { module.files += TestFile.createCommitted(it, module) }

    return TestCase(
        id = TestCaseId.Named(moduleName),
        kind = kind,
        modules = setOf(module),
        freeCompilerArgs = freeCompilerArgs,
        nominalPackageName = PackageName(moduleName),
        checks = checks,
        extras = extras,
    ).apply {
        initialize(givenDependencies, null)
    }
}

private fun AbstractNativeSimpleTest.compileToLibrary(
    testCase: TestCase,
    outputDir: File,
    dependencies: List<TestCompilationDependency<*>>
): TestCompilationResult.Success<out TestCompilationArtifact.KLIB> {
    val compilation = LibraryCompilation(
        settings = testRunSettings,
        freeCompilerArgs = testCase.freeCompilerArgs,
        sourceModules = testCase.modules,
        dependencies = dependencies,
        expectedArtifact = getLibraryArtifact(testCase, outputDir)
    )
    return compilation.result.assertSuccess()
}

private fun AbstractNativeSimpleTest.compileToExecutableInOneStage(
    testCase: TestCase,
    tryPassSystemCacheDirectory: Boolean,
    dependencies: List<TestCompilationDependency<*>>
): TestCompilationResult<out TestCompilationArtifact.Executable> {
    val compilation = ExecutableCompilation(
        settings = testRunSettings,
        freeCompilerArgs = testCase.freeCompilerArgs,
        sourceModules = testCase.modules,
        extras = testCase.extras,
        dependencies = dependencies,
        expectedArtifact = getExecutableArtifact(),
        tryPassSystemCacheDirectory = tryPassSystemCacheDirectory
    )
    return compilation.result
}

fun getLibraryArtifact(testCase: TestCase, outputDir: File, packed: Boolean = true) =
    TestCompilationArtifact.KLIB(outputDir.resolve(testCase.modules.first().name + if (packed) ".klib" else ""))

private fun AbstractNativeSimpleTest.getExecutableArtifact() =
    TestCompilationArtifact.Executable(buildDir.resolve("app." + testRunSettings.get<KotlinNativeTargets>().testTarget.family.exeSuffix))

private fun directiveValues(testDataFileContents: String, directive: String) =
    InTextDirectivesUtils.findListWithPrefixes(testDataFileContents, "// $directive: ")

fun AbstractNativeSimpleTest.muteTestIfNecessary(testDataFile: File) = muteTestIfNecessary(FileUtil.loadFile(testDataFile))
internal fun AbstractNativeSimpleTest.muteTestIfNecessary(testDataFileContents: String) {
    val pipelineType = testRunSettings.get<PipelineType>()
    val mutedWhenValues = directiveValues(testDataFileContents, TestDirectives.MUTED_WHEN.name)
    Assumptions.assumeFalse(mutedWhenValues.any { it == pipelineType.mutedOption.name })
}

internal fun AbstractNativeSimpleTest.firIdentical(testDataFile: File) =
     InTextDirectivesUtils.isDirectiveDefined(FileUtil.loadFile(testDataFile), TestDirectives.FIR_IDENTICAL.name)

internal fun AbstractNativeSimpleTest.freeCompilerArgs(testDataFile: File) = freeCompilerArgs(FileUtil.loadFile(testDataFile))
internal fun AbstractNativeSimpleTest.freeCompilerArgs(testDataFileContents: String) =
    directiveValues(testDataFileContents, TestDirectives.FREE_COMPILER_ARGS.name)
