/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.konan.test.blackbox.support.LoggedData
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.CInteropCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact.KLIB
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationDependency
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UsePartialLinkage
import org.jetbrains.kotlin.konan.test.blackbox.support.group.isDisabledNative
import org.jetbrains.kotlin.konan.test.blackbox.support.group.isIgnoredTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.CacheMode
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.OptimizationMode
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.ThreadStateChecker
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertFalse
import org.jetbrains.kotlin.test.services.impl.RegisteredDirectivesParser
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.fail
import java.io.File
import java.security.MessageDigest
import kotlin.io.path.Path

@Tag("caches")
@UsePartialLinkage(UsePartialLinkage.Mode.ERROR)
abstract class AbstractNativeIncrementalCompilationTest : AbstractNativeSimpleTest() {

    @BeforeEach
    fun assumeCachesAreEnabled() {
        Assumptions.assumeFalse(testRunSettings.get<CacheMode>() == CacheMode.WithoutCache)
        Assumptions.assumeFalse(testRunSettings.get<ThreadStateChecker>() == ThreadStateChecker.ENABLED)
    }

    protected fun runTest(@TestDataFile testDir: String) {
        val testDataDir = getAbsoluteFile(testDir)
        val testStructure = extractTestStructure(testDataDir)

        val sourceDirs = testStructure.modules.values.map { it.sourceDir }
        initializeBuildDirs(sourceDirs + listOf(externalLibsDir, autoCacheDir, icCacheDir))
        StepsExecutor(testStructure).execute()
    }

    private data class TestStructure(
        val projectInfo: ProjectInfo,
        val modules: Map<String, Module>,
    ) {
        data class Module(val moduleInfo: ModuleInfo, val testDataDir: File, val sourceDir: File) {
            fun getStep(stepId: Int): ModuleInfo.ModuleStep? = moduleInfo.steps[stepId]
        }
    }

    private fun extractTestStructure(testDataPath: File): TestStructure {
        val projectInfoFile = testDataPath.resolve(PROJECT_INFO_FILE)
        val directives = projectInfoFile.parseDirectives()
        Assumptions.assumeFalse(testRunSettings.isDisabledNative(directives))
        Assumptions.assumeFalse(testRunSettings.isIgnoredTarget(directives))

        val projectInfo = ProjectInfoParser(projectInfoFile).parse(testDataPath.name)

        val modules = projectInfo.modules.associateWith { moduleName ->
            val moduleDirectory = testDataPath.resolve(moduleName)
            val moduleInfo = moduleDirectory.resolve(MODULE_INFO_FILE)
            val parsedModule = ModuleInfoParser(
                moduleInfo,
                expectedStateDirectives = NativeCacheExpectation.byDirective.keys + OutputExpectation.byDirective.keys,
            ).parse(moduleName)

            val moduleSourceDir = buildDir.resolve(moduleName)
            TestStructure.Module(
                parsedModule,
                testDataDir = moduleDirectory,
                sourceDir = moduleSourceDir,
            )
        }
        return TestStructure(
            projectInfo = projectInfo,
            modules = modules,
        )
    }

    private inner class StepsExecutor(
        private val testStructure: TestStructure,
    ) {
        private val producedLibraries = mutableMapOf<String, KLIB>()
        private var previousSnapshot: Map<CacheKey, CacheEntry> = emptyMap()

        fun execute() {
            testStructure.projectInfo.steps.forEach { runStep(it) }
        }

        private fun runStep(step: ProjectInfo.ProjectBuildStep) {
            applyModifications(step)
            compileLibraries(step)

            val dumpBuiltCachesToPath = icCacheDir.resolve("__ic_build_output_${step.id}.txt")
            val [testCase, compilationResult] = compileMainExecutable(step.id, dumpBuiltCachesToPath)
            verifyOutputExpectations(step.id, compilationResult)

            // If the compilation is expected to fail, there is neither a cache to inspect nor an executable to run.
            if (expectsCompilationFailure(step.id)) return

            val executable = CompiledExecutable(testCase, compilationResult.assertSuccess())

            val buildOutputEntries = if (dumpBuiltCachesToPath.exists())
                dumpBuiltCachesToPath.useLines { lines -> lines.filter(String::isNotBlank).toSet() }
            else
                emptySet()

            val currentSnapshot = takeCacheSnapshot(step.id)
            verifyCacheExpectations(step.id, previousSnapshot, currentSnapshot, buildOutputEntries)
            previousSnapshot = currentSnapshot

            runExecutableAndVerify(executable.testCase, executable.testExecutable)
        }

        private fun applyModifications(step: ProjectInfo.ProjectBuildStep) {
            for (module in testStructure.modules.values) {
                val moduleStep = module.getStep(step.id) ?: continue
                moduleStep.modifications.forEach { modification ->
                    modification.execute(
                        testDirectory = module.testDataDir,
                        sourceDirectory = module.sourceDir,
                    )
                }
            }
        }

        private fun compileLibraries(step: ProjectInfo.ProjectBuildStep) {
            for (moduleName in step.order) {
                if (moduleName == MAIN_MODULE_NAME) continue
                val module = testStructure.modules.getValue(moduleName)
                val moduleStep = module.getStep(step.id)
                    ?: fail("Module '$moduleName' is in step ${step.id} order but has no module-info entry")
                val defFile = module.sourceDir.listFiles()?.singleOrNull { it.extension == "def" }
                producedLibraries[moduleName] = if (defFile != null) {
                    compileCInteropLibrary(moduleName, defFile)
                } else {
                    compileToLibrary(
                        module.sourceDir,
                        outputDir(moduleName),
                        freeCompilerArgs = TestCompilerArgs(
                            // otherwise, it might shadow some problems with inline functions.
                            listOf("-XXLanguage:-IrIntraModuleInlinerBeforeKlibSerialization") + moduleStep.cliArguments
                        ),
                        dependencies = moduleStep.dependencies.toCompilationDependencies(),
                    )
                }
            }
        }

        private fun compileCInteropLibrary(moduleName: String, defFile: File): KLIB =
            CInteropCompilation(
                settings = testRunSettings,
                freeCompilerArgs = TestCompilerArgs.EMPTY,
                defFile = defFile,
                dependencies = emptyList(),
                expectedArtifact = KLIB(outputDir(moduleName).resolve("$moduleName.klib")),
            ).result.assertSuccess().resultingArtifact

        // Compile test, NOT respecting possible `mode=TWO_STAGE_MULTI_MODULE`: don't add intermediate LibraryCompilation(kt->klib).
        // KT-66014: Extract this test from usual Native test run, and run it in scope of new test module
        private fun compileMainExecutable(
            stepId: Int,
            dumpBuiltCachesToPath: File,
        ): Pair<TestCase, TestCompilationResult<out TestCompilationArtifact.Executable>> {
            val mainModule = testStructure.modules.getValue(MAIN_MODULE_NAME)
            val mainStep = mainModule.getStep(stepId) ?: fail("Main module has no module-info entry")
            listOf(externalLibsDir, autoCacheDir, icCacheDir).forEach { it.mkdirs() }
            val testCase = generateTestCaseWithSingleModule(
                mainModule.sourceDir,
                TestCompilerArgs(
                    listOf(
                        "-Xauto-cache-from=${externalLibsDir.absolutePath}",
                        "-Xauto-cache-dir=${autoCacheDir.absolutePath}",
                        "-Xic-cache-dir=${icCacheDir.absolutePath}",
                        "-Xdump-built-caches-to=${dumpBuiltCachesToPath.absolutePath}",
                        "-Xenable-incremental-compilation",
                        "-verbose",
                    ) + mainStep.cliArguments
                ),
            )
            val compilationResult = compileToExecutableInOneStage(
                testCase,
                tryPassSystemCacheDirectory = false,
                dependencies = mainStep.dependencies.toCompilationDependencies(),
            )
            return testCase to compilationResult
        }

        private fun Collection<ModuleInfo.Dependency>.toCompilationDependencies(): List<TestCompilationDependency<*>> =
            mapNotNull { dep ->
                val library = producedLibraries.getValue(dep.moduleName)
                if (dep.isFriend) library.asFriendLibraryDependency() else library.asLibraryDependency()
            }

        private fun takeCacheSnapshot(stepId: Int): Map<CacheKey, CacheEntry> = buildMap {
            for ([moduleName, module] in testStructure.modules) {
                if (moduleName == MAIN_MODULE_NAME) continue
                val expectedFiles = module.getStep(stepId)?.expectedFileStats ?: continue
                expectedFiles.filterKeys { it in NativeCacheExpectation.byDirective }.values.flatten().toSet().forEach { relativePath ->
                    val key = CacheKey(moduleName, relativePath)
                    val sourceFile = module.sourceDir.resolve(relativePath)
                    val cacheDir = when {
                        // C-interop libraries are cached monolithically, so a def file maps to the whole-library cache.
                        relativePath.endsWith(".def") -> monolithicLibraryCache(moduleName)
                        sourceFile.exists() -> libraryFileCache(moduleName, relativePath, sourceFile.packageFqName())
                        else -> previousSnapshot[key]?.cacheDir
                            ?: fail("No previous cache entry data for removed source file $moduleName/$relativePath at step $stepId")
                    }
                    put(key, CacheEntry.create(cacheDir))
                }
            }
        }

        private fun verifyCacheExpectations(
            stepId: Int,
            previous: Map<CacheKey, CacheEntry>,
            current: Map<CacheKey, CacheEntry>,
            buildOutputEntries: Set<String>
        ) {
            val expectedInBuildOutput = mutableSetOf<String>()
            for ([moduleName, module] in testStructure.modules) {
                val expected = module.getStep(stepId)?.expectedFileStats ?: continue
                for ([directive, files] in expected) {
                    val cacheExpectation = NativeCacheExpectation.byDirective[directive] ?: continue
                    files.forEach { path ->
                        verifyExpectation(stepId, moduleName, path, cacheExpectation, previous, current, buildOutputEntries)
                        if (cacheExpectation == NativeCacheExpectation.ADDED_CACHE ||
                            cacheExpectation == NativeCacheExpectation.MODIFIED_CACHE
                        ) {
                            current[CacheKey(moduleName, path)]?.cacheDir?.let { dir ->
                                expectedInBuildOutput.add(archivePath(dir))
                            }
                        }
                    }
                }
            }

            val missing = expectedInBuildOutput - buildOutputEntries
            JUnit5Assertions.assertTrue(missing.isEmpty()) {
                val unexpected = buildOutputEntries - expectedInBuildOutput
                "IC build output missing expected entries at step $stepId:\n  missing: $missing, unexpected: $unexpected"
            }
        }

        private fun outputExpectations(stepId: Int): Map<String, Set<String>> {
            val mainModule = testStructure.modules.getValue(MAIN_MODULE_NAME)
            val expectedFileStats = mainModule.getStep(stepId)?.expectedFileStats ?: return emptyMap()
            return expectedFileStats.filterKeys { it in OutputExpectation.byDirective }
        }

        private fun expectsCompilationFailure(stepId: Int): Boolean =
            OutputExpectation.EXPECTED_FAILURE_OUTPUT.directive in outputExpectations(stepId)

        private fun verifyOutputExpectations(
            stepId: Int,
            compilationResult: TestCompilationResult<out TestCompilationArtifact.Executable>,
        ) {
            val outputExpectations = outputExpectations(stepId)
            if (outputExpectations.isEmpty()) return

            if (expectsCompilationFailure(stepId)) {
                assertTrue(compilationResult is TestCompilationResult.Failure) {
                    "Expected the main executable compilation at step $stepId to fail, but it succeeded"
                }
            }

            val compilerOutput = ((compilationResult as? TestCompilationResult.ImmediateResult<*>)
                ?.loggedData as? LoggedData.CompilationToolCall)?.toolOutput
                ?: fail("No compiler output captured for the main executable compilation at step $stepId")

            val mainModule = testStructure.modules.getValue(MAIN_MODULE_NAME)
            for ([directive, fileNames] in outputExpectations) {
                val expectation = OutputExpectation.byDirective.getValue(directive)
                for (fileName in fileNames) {
                    val patternsFile = mainModule.testDataDir.resolve(fileName)
                    assertTrue(patternsFile.exists()) { "Patterns file does not exist: $patternsFile" }
                    patternsFile.readLines()
                        .map(String::trim)
                        .filter { it.isNotEmpty() && !it.startsWith("//") }
                        .forEach { pattern ->
                            when (expectation) {
                                OutputExpectation.EXPECTED_OUTPUT,
                                OutputExpectation.EXPECTED_FAILURE_OUTPUT,
                                    -> assertTrue(compilerOutput.contains(pattern)) {
                                    "Expected the compiler output at step $stepId to contain \"$pattern\", but it did not:\n$compilerOutput"
                                }
                                OutputExpectation.FORBIDDEN_OUTPUT -> assertFalse(compilerOutput.contains(pattern)) {
                                    "Expected the compiler output at step $stepId to NOT contain \"$pattern\", but it did:\n$compilerOutput"
                                }
                            }
                        }
                }
            }
        }

        private fun verifyExpectation(
            stepId: Int,
            moduleName: String,
            path: String,
            expectation: NativeCacheExpectation,
            previous: Map<CacheKey, CacheEntry>,
            current: Map<CacheKey, CacheEntry>,
            buildOutputEntries: Set<String>
        ) {
            val location = "${moduleName}/${path} at step $stepId"
            val cacheKey = CacheKey(moduleName, path)
            val previousCacheEntry = previous[cacheKey]
            val currentCacheEntry = current[cacheKey]

            fun CacheEntry?.required(): CacheEntry =
                requireNotNull(this) { "No cache entry data for $location" }

            when (expectation) {
                NativeCacheExpectation.ADDED_CACHE -> {
                    val currentCache = currentCacheEntry.required()
                    assertTrue(currentCache.cacheDir.exists()) { "Expected added cache to exist: ${currentCache.cacheDir}" }
                    assertTrue(archivePath(currentCache.cacheDir) in buildOutputEntries) {
                        "Expected $location to appear in IC build output after build, but it did not."
                    }
                }
                NativeCacheExpectation.MODIFIED_CACHE -> {
                    val previousCache = previousCacheEntry.required()
                    val currentCache = currentCacheEntry.required()
                    assertTrue(currentCache.cacheDir.exists()) { "Expected modified cache to exist: ${currentCache.cacheDir}" }
                    assertFalse(previousCache.hasSameContentAs(currentCache)) { "Expected cache to be modified for $location" }
                    assertTrue(archivePath(currentCache.cacheDir) in buildOutputEntries) {
                        "Expected modified $location to appear in IC build output, but it did not."
                    }
                }
                NativeCacheExpectation.UNCHANGED_CACHE -> {
                    val previousCache = previousCacheEntry.required()
                    val currentCache = currentCacheEntry.required()
                    assertTrue(currentCache.cacheDir.exists()) { "Expected unchanged cache to exist: ${currentCache.cacheDir}" }
                    assertTrue(previousCache.hasSameContentAs(currentCache)) { "Expected cache to stay unchanged for $location" }
                    assertFalse(archivePath(currentCache.cacheDir) in buildOutputEntries) {
                        "Did not expect unchanged $location to appear in IC build output."
                    }
                }
                NativeCacheExpectation.REMOVED_CACHE -> {
                    val previousCache = previousCacheEntry.required()
                    assertFalse(previousCache.cacheDir.exists()) { "Expected cache to be removed: ${previousCache.cacheDir}" }
                    assertFalse(archivePath(previousCache.cacheDir) in buildOutputEntries) {
                        "Did not expect removed $location to appear in IC build output."
                    }
                }
            }
        }
    }

    // ---- cache helpers -------------------------------------------------------------

    private val externalLibsDir: File get() = buildDir.resolve("external")
    private val autoCacheDir: File get() = buildDir.resolve("__auto_cache__")
    private val icCacheDir: File get() = buildDir.resolve("__ic_cache__")

    private val cacheFlavor: String
        get() = CacheMode.computeCacheDirName(
            testRunSettings.get<KotlinNativeTargets>().testTarget,
            "STATIC",
            testRunSettings.get<OptimizationMode>() == OptimizationMode.DEBUG,
            testRunSettings.get<OptimizationMode>() == OptimizationMode.OPT,
            checkStateAtExternalCalls = testRunSettings.get<ThreadStateChecker>() == ThreadStateChecker.ENABLED,
        )

    private fun archivePath(cacheDir: File): String =
        Path(cacheDir.absolutePath, "bin", "lib${cacheDir.name}.a").toString()

    private fun monolithicLibraryCache(libName: String): File =
        icCacheDir.resolve(cacheFlavor).resolve("$libName-cache")

    private fun libraryFileCache(libName: String, libFileRelativePath: String, fqName: String): File {
        val libCacheDir = icCacheDir.resolve(cacheFlavor).resolve("$libName-per-file-cache")
        val absoluteSourcePath = buildDir.resolve(libName).resolve(libFileRelativePath).absolutePath
        val fileId = cacheFileId(fqName, absoluteSourcePath)
        return libCacheDir.resolve(fileId)
    }

    private fun cacheFileId(fqName: String, filePath: String) =
        "${fqName.ifEmpty { "ROOT" }}.${filePath.hashCode().toString(Character.MAX_RADIX)}"

    private data class CacheKey(val moduleName: String, val relativePath: String)
    private data class CacheEntry(
        val cacheDir: File,
        val files: Map<String, CacheFile> = emptyMap(),
    ) {
        data class CacheFile(
            val size: Long,
            val lastModified: Long,
            val hash: String,
        )

        fun hasSameContentAs(other: CacheEntry): Boolean {
            if (files.keys != other.files.keys) return false
            val metadataIsEqual = files.all { [path, file] ->
                val otherFile = other.files.getValue(path)
                file.size == otherFile.size && file.lastModified == otherFile.lastModified
            }
            if (!metadataIsEqual) return false

            return files.all { [path, file] -> file.hash == other.files.getValue(path).hash }
        }

        companion object {
            fun create(cacheDir: File): CacheEntry = CacheEntry(cacheDir, cacheDir.cacheFiles())

            private fun File.cacheFiles(): Map<String, CacheFile> =
                if (!exists()) emptyMap()
                else walkTopDown()
                    .filter { it.isFile }
                    .associate {
                        val relativePath = it.relativeTo(this).invariantSeparatorsPath
                        relativePath to CacheFile(
                            size = it.length(),
                            lastModified = it.lastModified(),
                            hash = it.sha256(),
                        )
                    }

            private fun File.sha256(): String =
                MessageDigest.getInstance("SHA-256")
                    .digest(readBytes())
                    .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        }
    }

    // ---- helpers ---------------------------------------------------------------------------

    private fun File.packageFqName(): String =
        useLines { lines ->
            lines.firstNotNullOfOrNull { line ->
                PACKAGE_DIRECTIVE_REGEX.find(line)?.groupValues?.get(1)
            }
        }.orEmpty()

    private fun File.parseDirectives() =
        RegisteredDirectivesParser(TestDirectives, JUnit5Assertions).also { parser ->
            forEachLine { parser.parse(it) }
        }.build()

    private fun initializeBuildDirs(dirs: List<File>) {
        dirs.forEach {
            it.deleteRecursively()
            it.mkdirs()
        }
    }

    private fun outputDir(moduleName: String) = if (moduleName.startsWith(EXTERNAL_MODULE_NAME_PREFIX)) externalLibsDir else buildDir

    companion object {
        private const val MAIN_MODULE_NAME = "main"
        private const val EXTERNAL_MODULE_NAME_PREFIX = "external"

        private val PACKAGE_DIRECTIVE_REGEX = Regex("""^\s*package\s+([\w.]+)""")

        private enum class NativeCacheExpectation(val directive: String) {
            ADDED_CACHE("added cache"),
            MODIFIED_CACHE("modified cache"),
            UNCHANGED_CACHE("unchanged cache"),
            REMOVED_CACHE("removed cache");

            companion object {
                val byDirective: Map<String, NativeCacheExpectation> = entries.associateBy { it.directive }
            }
        }

        // Expectations about the compiler output of the main executable compilation.
        // The directive value is a file (in the main module's test data directory) whose non-blank,
        // non-comment lines are substrings that must (or must not) occur in the compiler output.
        // "expected compilation failure" additionally requires the compilation to fail; such a step
        // neither checks the caches nor runs the produced executable.
        private enum class OutputExpectation(val directive: String) {
            EXPECTED_OUTPUT("expected output"),
            FORBIDDEN_OUTPUT("forbidden output"),
            EXPECTED_FAILURE_OUTPUT("expected compilation failure");

            companion object {
                val byDirective: Map<String, OutputExpectation> = entries.associateBy { it.directive }
            }
        }
    }
}
