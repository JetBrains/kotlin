/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact.KLIB
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationDependency
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
                expectedStateDirectives = NativeCacheExpectation.byDirective.keys,
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
            val executable = compileMainExecutable(step.id)

            val currentSnapshot = takeCacheSnapshot(step.id)
            verifyCacheExpectations(step.id, previousSnapshot, currentSnapshot)
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
                producedLibraries[moduleName] = compileToLibrary(
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

        // Compile test, NOT respecting possible `mode=TWO_STAGE_MULTI_MODULE`: don't add intermediate LibraryCompilation(kt->klib).
        // KT-66014: Extract this test from usual Native test run, and run it in scope of new test module
        private fun compileMainExecutable(stepId: Int): CompiledExecutable {
            val mainModule = testStructure.modules.getValue(MAIN_MODULE_NAME)
            val mainStep = mainModule.getStep(stepId) ?: fail("Main module has no module-info entry")
            listOf(externalLibsDir, autoCacheDir, icCacheDir).forEach { it.mkdirs() }
            return compileToExecutableInOneStage(
                mainModule.sourceDir,
                tryPassSystemCacheDirectory = false,
                freeCompilerArgs = TestCompilerArgs(
                    listOf(
                        "-Xauto-cache-from=${externalLibsDir.absolutePath}",
                        "-Xauto-cache-dir=${autoCacheDir.absolutePath}",
                        "-Xic-cache-dir=${icCacheDir.absolutePath}",
                        "-Xenable-incremental-compilation",
                        "-verbose",
                    ) + mainStep.cliArguments
                ),
                dependencies = mainStep.dependencies.toCompilationDependencies(),
            )
        }

        private fun Collection<ModuleInfo.Dependency>.toCompilationDependencies(): List<TestCompilationDependency<*>> =
            mapNotNull { dep ->
                val library = producedLibraries.getValue(dep.moduleName)
                if (dep.isFriend) library.asFriendLibraryDependency() else library.asLibraryDependency()
            }

        private fun takeCacheSnapshot(stepId: Int): Map<CacheKey, CacheEntry> = buildMap {
            for ((moduleName, module) in testStructure.modules) {
                if (moduleName == MAIN_MODULE_NAME) continue
                val expectedFiles = module.getStep(stepId)?.expectedFileStats ?: continue
                expectedFiles.values.flatten().toSet().forEach { relativePath ->
                    val key = CacheKey(moduleName, relativePath)
                    val sourceFile = module.sourceDir.resolve(relativePath)
                    val cacheDir = when {
                        sourceFile.exists() -> libraryFileCache(moduleName, relativePath, sourceFile.packageFqName())
                        else -> previousSnapshot[key]?.cacheDir
                            ?: fail("No previous cache entry data for removed source file $moduleName/$relativePath at step $stepId")
                    }
                    put(key, CacheEntry.create(cacheDir))
                }
            }
        }

        private fun verifyCacheExpectations(stepId: Int, previous: Map<CacheKey, CacheEntry>, current: Map<CacheKey, CacheEntry>) {
            for ((moduleName, module) in testStructure.modules) {
                val expected = module.getStep(stepId)?.expectedFileStats ?: continue
                for ((directive, files) in expected) {
                    val cacheExpectation = NativeCacheExpectation.byDirective.getValue(directive)
                    files.forEach { path ->
                        verifyExpectation(stepId, moduleName, path, cacheExpectation, previous, current)
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
                }
                NativeCacheExpectation.MODIFIED_CACHE -> {
                    val previousCache = previousCacheEntry.required()
                    val currentCache = currentCacheEntry.required()
                    assertTrue(currentCache.cacheDir.exists()) { "Expected modified cache to exist: ${currentCache.cacheDir}" }
                    assertFalse(previousCache.hasSameContentAs(currentCache)) { "Expected cache to be modified for $location" }
                }
                NativeCacheExpectation.UNCHANGED_CACHE -> {
                    val previousCache = previousCacheEntry.required()
                    val currentCache = currentCacheEntry.required()
                    assertTrue(currentCache.cacheDir.exists()) { "Expected unchanged cache to exist: ${currentCache.cacheDir}" }
                    assertTrue(previousCache.hasSameContentAs(currentCache)) { "Expected cache to stay unchanged for $location" }
                }
                NativeCacheExpectation.REMOVED_CACHE -> {
                    val previousCache = previousCacheEntry.required()
                    assertFalse(previousCache.cacheDir.exists()) { "Expected cache to be removed: ${previousCache.cacheDir}" }
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
            checkStateAtExternalCalls = testRunSettings.get<ThreadStateChecker>() == ThreadStateChecker.ENABLED,
        )

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
            val metadataIsEqual = files.all { (path, file) ->
                val otherFile = other.files.getValue(path)
                file.size == otherFile.size && file.lastModified == otherFile.lastModified
            }
            if (!metadataIsEqual) return false

            return files.all { (path, file) -> file.hash == other.files.getValue(path).hash }
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
    }
}
