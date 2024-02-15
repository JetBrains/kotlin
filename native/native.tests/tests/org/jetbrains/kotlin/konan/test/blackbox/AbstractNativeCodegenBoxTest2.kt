/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.test.blackbox.support.TestModule
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.CompilationToolException
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationFactory
import org.jetbrains.kotlin.konan.test.blackbox.support.group.ExtTestDataFile
import org.jetbrains.kotlin.konan.test.blackbox.support.group.ExtTestDataFileStructureFactory
import org.jetbrains.kotlin.konan.test.blackbox.support.group.isIgnoredTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.*
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.BaseDirs
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Binaries
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.ExternalSourceTransformersProvider
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Settings
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.TestRoots
import org.jetbrains.kotlin.konan.test.blackbox.support.util.*
import org.jetbrains.kotlin.konan.test.blackbox.support.util.ThreadSafeCache
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import java.io.File

//@ExtendWith(NativeBlackBoxTestSupport::class)
//@EnforcedProperty(ClassLevelProperty.)
internal abstract class AbstractNativeCodegenBoxTest2 : AbstractNativeSimpleTest(), ExternalSourceTransformersProvider {
    private val structureFactory = ExtTestDataFileStructureFactory(parentDisposable = null)
    private val sharedModules = ThreadSafeCache<String, TestModule.Shared?>()
    private val compilationFactory = TestCompilationFactory()
    private val compilationFactoryReversed = TestCompilationFactory()

    /**
     * Run JUnit test.
     *
     * This function should be called from a method annotated with [org.junit.jupiter.api.Test].
     */
    fun runTest(@TestDataFile testDataFilePath: String) {
        val settings = computeSettings()
        val absoluteTestFile = getAbsoluteFile(testDataFilePath)
        val irDumpsBasePath = settings.get<Binaries>().testBinariesDir.absolutePath + "/" + testDataFilePath.replace("/", "_").removeSuffix(".kt")
        File(irDumpsBasePath).mkdirs()
        val irDumpFile = irDumpsBasePath + "/ir.txt"
        val irDumpFileReversed = irDumpsBasePath + "/ir.reversed.txt"

//        val testCaseIdReversed = TestCaseId.Named
        try {
            val extTestDataFile = ExtTestDataFile(
                testDataFile = absoluteTestFile,
                structureFactory = structureFactory,
                customSourceTransformers = this.getSourceTransformers(absoluteTestFile),
                settings = settings,
                additionalCompilerArgs = listOf("-Xbinary=dumpIrAndStopAfterLowerings=$irDumpFile")
            )

            if (!extTestDataFile.isRelevant) return

            val testCase = extTestDataFile.createTestCase(
                settings = settings,
                sharedModules = sharedModules
            )
            compilationFactory.testCasesToExecutable(listOf(testCase), settings).result

            val extTestDataFileReversed = ExtTestDataFile(
                testDataFile = absoluteTestFile,
                structureFactory = structureFactory,
                customSourceTransformers = this.getSourceTransformers(absoluteTestFile),
                settings = settings,
                additionalCompilerArgs = listOf("-Xbinary=dumpIrAndStopAfterLowerings=$irDumpFileReversed", "-Xbinary=reverseFilesWhenLowering=true")
            )

            val testCaseReversed = extTestDataFileReversed.createTestCase(
                settings = settings,
                sharedModules = sharedModules
            )
            compilationFactoryReversed.testCasesToExecutable(listOf(testCaseReversed), settings).result

            KotlinTestUtils.assertEqualsToFile(File(irDumpFile), File(irDumpFileReversed).readText())
        } catch (e: CompilationToolException) {
            // TODO find out the way not to re-read test source file, but to re-use already extracted test directives.
            if (testRunSettings.isIgnoredTarget(absoluteTestFile))
                println("There was an expected failure: CompilationToolException: ${e.reason}")
            else
                fail { e.reason }
        }
    }
    private val registeredSourceTransformers: ThreadSafeCache<File, MutableList<ExternalSourceTransformer>> = ThreadSafeCache()

    override fun getSourceTransformers(testDataFile: File): ExternalSourceTransformers? = registeredSourceTransformers[testDataFile]

    /**
     * Called directly from test class constructor.
     */
    fun register(@TestDataFile testDataFilePath: String, sourceTransformer: ExternalSourceTransformer) {
        registeredSourceTransformers.computeIfAbsent(getAbsoluteFile(testDataFilePath)) { mutableListOf() } += sourceTransformer
    }

    private fun computeSettings(): Settings {
        val newSettings = buildList<Any> {
            val enclosingTestClass = this@AbstractNativeCodegenBoxTest2::class.java
            val testRoots = computeTestRoots(enclosingTestClass)
            add(testRoots)

            val baseDirs = testRunSettings.get<BaseDirs>()
            val targets = testRunSettings.get<KotlinNativeTargets>()
            val generatedSources = computeGeneratedSourceDirs(baseDirs, targets, enclosingTestClass)
            add(generatedSources)

            val binaries = computeBinariesForBlackBoxTests(baseDirs, targets, this@AbstractNativeCodegenBoxTest2::class.java)
            add(binaries)

            add(CacheMode.Alias.STATIC_ONLY_DIST)
        }

        return object : Settings(testRunSettings, newSettings) {}
    }

    private fun computeTestRoots(enclosingTestClass: Class<*>): TestRoots {
        fun TestMetadata.testRoot() = getAbsoluteFile(localPath = value)

        val testRoots: Set<File> = when (val outermostTestMetadata = enclosingTestClass.getAnnotation(TestMetadata::class.java)) {
            null -> {
                enclosingTestClass.declaredClasses.mapNotNullToSet { nestedClass ->
                    nestedClass.getAnnotation(TestMetadata::class.java)?.testRoot()
                }
            }
            else -> setOf(outermostTestMetadata.testRoot())
        }

        val baseDir: File = when (testRoots.size) {
            0 -> fail { "No test roots found for $enclosingTestClass test class." }
            1 -> testRoots.first().parentFile
            else -> {
                val baseDirs = testRoots.mapToSet { it.parentFile }
                JUnit5Assertions.assertEquals(1, baseDirs.size) {
                    "Controversial base directories computed for test roots for $enclosingTestClass test class: $baseDirs"
                }

                baseDirs.first()
            }
        }

        return TestRoots(testRoots, baseDir)
    }

    private fun computeGeneratedSourceDirs(
        baseDirs: BaseDirs,
        targets: KotlinNativeTargets,
        enclosingTestClass: Class<*>
    ): GeneratedSources {
        val testSourcesDir = baseDirs.testBuildDir
            .resolve("bb.src") // "bb" for black box
            .resolve("${targets.testTarget.compressedName}_${enclosingTestClass.compressedSimpleName}")
//            .ensureExistsAndIsEmptyDirectory() // Clean-up the directory with all potentially stale generated sources.

        val sharedSourcesDir = testSourcesDir
            .resolve(SHARED_MODULES_DIR_NAME)
//            .ensureExistsAndIsEmptyDirectory()

        return GeneratedSources(testSourcesDir, sharedSourcesDir)
    }

    /** See also [computeBinariesForSimpleTests] */
    private fun computeBinariesForBlackBoxTests(
        baseDirs: BaseDirs,
        targets: KotlinNativeTargets,
        enclosingTestClass: Class<*>
    ): Binaries {
        val testBinariesDir = baseDirs.testBuildDir
            .resolve("bb.out") // "bb" for black box
            .resolve("${targets.testTarget.compressedName}_${enclosingTestClass.compressedSimpleName}")
//            .ensureExistsAndIsEmptyDirectory() // Clean-up the directory with all potentially stale artifacts.

        return Binaries(
            testBinariesDir = testBinariesDir,
            lazySharedBinariesDir = { testBinariesDir.resolve(SHARED_MODULES_DIR_NAME)/*.ensureExistsAndIsEmptyDirectory()*/ },
            lazyGivenBinariesDir = { testBinariesDir.resolve(GIVEN_MODULES_DIR_NAME)/*.ensureExistsAndIsEmptyDirectory()*/ }
        )
    }
}
