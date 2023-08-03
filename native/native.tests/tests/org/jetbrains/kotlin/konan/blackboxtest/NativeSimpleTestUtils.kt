/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.konan.blackboxtest.support.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.Binaries
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.PipelineType
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.Timeouts
import org.jetbrains.kotlin.konan.blackboxtest.support.util.LAUNCHER_MODULE_NAME
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.junit.jupiter.api.Assumptions
import java.io.File

internal abstract class ArtifactBuilder<T>(
    val test: AbstractNativeSimpleTest,
    val rootDir: File,
    val targetSrc: String,
    dependencies: List<TestCompilationArtifact.KLIB>
) {
    private val buildDir = test.buildDir
    var outputDir: String = ""

    private val sources = mutableListOf<Pair<String, String>>()
    private val dependencies = dependencies.toMutableList()

    infix fun String.copyTo(to: String) {
        sources.add(Pair(this, to))
    }

    fun dependsOn(klib: TestCompilationArtifact.KLIB) {
        dependencies.add(klib)
    }

    protected abstract fun build(sourcesDir: File, outputDir: File, dependencies: List<TestCompilationArtifact.KLIB>): T

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
    dependencies: List<TestCompilationArtifact.KLIB>
) : ArtifactBuilder<TestCompilationArtifact.KLIB>(test, rootDir, targetSrc, dependencies) {
    var libraryVersion: String? = null

    override fun build(sourcesDir: File, outputDir: File, dependencies: List<TestCompilationArtifact.KLIB>) =
        test.compileToLibrary(
            sourcesDir,
            outputDir,
            freeCompilerArgs = libraryVersion?.let { TestCompilerArgs(listOf("-library-version=$it")) } ?: TestCompilerArgs.EMPTY,
            dependencies
        )
}

internal class ExecutableBuilder(
    test: AbstractNativeSimpleTest,
    rootDir: File,
    targetSrc: String,
    val tryPassSystemCacheDirectory: Boolean,
    dependencies: List<TestCompilationArtifact.KLIB>
) : ArtifactBuilder<CompiledExecutable>(test, rootDir, targetSrc, dependencies) {
    private val freeCompilerArgs = mutableListOf<String>()

    operator fun String.unaryPlus() {
        freeCompilerArgs.add(this)
    }

    override fun build(sourcesDir: File, outputDir: File, dependencies: List<TestCompilationArtifact.KLIB>) =
        test.compileToExecutable(
            sourcesDir,
            tryPassSystemCacheDirectory,
            freeCompilerArgs = if (freeCompilerArgs.isEmpty()) TestCompilerArgs.EMPTY else TestCompilerArgs(freeCompilerArgs),
            dependencies
        )
}

internal val AbstractNativeSimpleTest.buildDir: File get() = testRunSettings.get<Binaries>().testBinariesDir
internal val AbstractNativeSimpleTest.targets: KotlinNativeTargets get() = testRunSettings.get()

internal fun TestCompilationArtifact.KLIB.asLibraryDependency() =
    ExistingDependency(this, TestCompilationDependencyType.Library)

internal fun TestCompilationArtifact.KLIB.asIncludedLibraryDependency() =
    ExistingDependency(this, TestCompilationDependencyType.IncludedLibrary)

internal fun TestCompilationArtifact.KLIBStaticCache.asStaticCacheDependency() =
    ExistingDependency(this, TestCompilationDependencyType.LibraryStaticCache)

internal fun AbstractNativeSimpleTest.compileToLibrary(sourcesDir: File, vararg dependencies: TestCompilationArtifact.KLIB) =
    compileToLibrary(sourcesDir, buildDir, *dependencies)

internal fun AbstractNativeSimpleTest.compileToLibrary(testCase: TestCase, vararg dependencies: TestCompilationDependency<*>) =
    compileToLibrary(testCase, buildDir, dependencies.asList())

internal fun AbstractNativeSimpleTest.compileToLibrary(
    sourcesDir: File,
    outputDir: File,
    vararg dependencies: TestCompilationArtifact.KLIB
): TestCompilationArtifact.KLIB = compileToLibrary(sourcesDir, outputDir, TestCompilerArgs.EMPTY, dependencies.asList())

internal fun AbstractNativeSimpleTest.compileToLibrary(
    sourcesDir: File,
    outputDir: File,
    freeCompilerArgs: TestCompilerArgs,
    dependencies: List<TestCompilationArtifact.KLIB>
): TestCompilationArtifact.KLIB {
    val testCase: TestCase = generateTestCaseWithSingleModule(sourcesDir, freeCompilerArgs)
    val compilationResult = compileToLibrary(testCase, outputDir, dependencies.map { it.asLibraryDependency() })
    return compilationResult.resultingArtifact
}

internal fun AbstractNativeSimpleTest.cinteropToLibrary(
    targets: KotlinNativeTargets,
    defFile: File,
    outputDir: File,
    freeCompilerArgs: TestCompilerArgs
): TestCompilationResult<out TestCompilationArtifact.KLIB> {
    val testCase: TestCase = generateCInteropTestCaseFromSingleDefFile(defFile, freeCompilerArgs)
    return CInteropCompilation(
        classLoader = testRunSettings.get(),
        targets = targets,
        freeCompilerArgs = freeCompilerArgs,
        defFile = testCase.modules.single().files.single().location,
        expectedArtifact = getLibraryArtifact(testCase, outputDir)
    ).result
}

internal class CompiledExecutable(
    val testCase: TestCase,
    val compilationResult: TestCompilationResult.Success<out TestCompilationArtifact.Executable>
) {
    val executableFile: File get() = compilationResult.resultingArtifact.executableFile

    val testExecutable by lazy { TestExecutable.fromCompilationResult(testCase, compilationResult) }
}

internal fun AbstractNativeSimpleTest.compileToExecutable(
    sourcesDir: File,
    tryPassSystemCacheDirectory: Boolean,
    freeCompilerArgs: TestCompilerArgs,
    vararg dependencies: TestCompilationArtifact.KLIB
) = compileToExecutable(sourcesDir, tryPassSystemCacheDirectory, freeCompilerArgs, dependencies.asList())

internal fun AbstractNativeSimpleTest.compileToExecutable(
    sourcesDir: File,
    tryPassSystemCacheDirectory: Boolean,
    freeCompilerArgs: TestCompilerArgs,
    dependencies: List<TestCompilationArtifact.KLIB>
): CompiledExecutable {
    val testCase: TestCase = generateTestCaseWithSingleModule(sourcesDir, freeCompilerArgs)
    val compilationResult = compileToExecutable(testCase, tryPassSystemCacheDirectory, dependencies.map { it.asLibraryDependency() })
    return CompiledExecutable(testCase, compilationResult.assertSuccess())
}

internal fun AbstractNativeSimpleTest.compileToExecutable(testCase: TestCase, vararg dependencies: TestCompilationDependency<*>) =
    compileToExecutable(testCase, true, dependencies.asList())

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
        expectedArtifact = TestCompilationArtifact.KLIBStaticCache(cacheDir, klib)
    )
    return compilation.result.assertSuccess().resultingArtifact
}

internal fun AbstractNativeSimpleTest.generateTestCaseWithSingleModule(
    moduleDir: File?,
    freeCompilerArgs: TestCompilerArgs = TestCompilerArgs.EMPTY
): TestCase {
    val moduleName: String = moduleDir?.name ?: LAUNCHER_MODULE_NAME
    val module = TestModule.Exclusive(moduleName, emptySet(), emptySet(), emptySet())

    moduleDir?.walkTopDown()
        ?.filter { it.isFile && it.extension == "kt" }
        ?.forEach { file -> module.files += TestFile.createCommitted(file, module) }

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

internal fun AbstractNativeSimpleTest.generateTestCaseWithSingleFile(
    sourceFile: File,
    moduleName: String = sourceFile.name,
    freeCompilerArgs: TestCompilerArgs = TestCompilerArgs.EMPTY,
    testKind: TestKind = TestKind.STANDALONE,
    extras: TestCase.Extras = TestCase.WithTestRunnerExtras(TestRunnerType.DEFAULT)
): TestCase {
    val module = TestModule.Exclusive(moduleName, emptySet(), emptySet(), emptySet())
    module.files += TestFile.createCommitted(sourceFile, module)

    return TestCase(
        id = TestCaseId.Named(moduleName),
        kind = testKind,
        modules = setOf(module),
        freeCompilerArgs = freeCompilerArgs,
        nominalPackageName = PackageName.EMPTY,
        checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout),
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

private fun AbstractNativeSimpleTest.compileToExecutable(
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

internal fun getLibraryArtifact(testCase: TestCase, outputDir: File) =
    TestCompilationArtifact.KLIB(outputDir.resolve(testCase.modules.first().name + ".klib"))

private fun AbstractNativeSimpleTest.getExecutableArtifact() =
    TestCompilationArtifact.Executable(buildDir.resolve("app." + testRunSettings.get<KotlinNativeTargets>().testTarget.family.exeSuffix))

private fun directiveValues(testDataFileContents: String, directive: String) =
    InTextDirectivesUtils.findListWithPrefixes(testDataFileContents, "// $directive: ")

internal fun AbstractNativeSimpleTest.muteTestIfNecessary(testDataFile: File) = muteTestIfNecessary(FileUtil.loadFile(testDataFile))
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
