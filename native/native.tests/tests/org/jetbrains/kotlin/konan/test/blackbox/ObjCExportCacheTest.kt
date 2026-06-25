/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.*
import org.jetbrains.kotlin.konan.test.blackbox.support.util.*
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

@Tag("caches")
@EnforcedHostTarget
@TestMetadata(ObjCExportCacheTest.TEST_SUITE_PATH)
@TestDataPath("\$PROJECT_ROOT")
class ObjCExportCacheTest : AbstractNativeSimpleTest() {

    private val testSuiteDir = ForTestCompileRuntime.transformTestDataPath(TEST_SUITE_PATH)
    private val extras = TestCase.NoTestRunnerExtras("There's no entrypoint in Swift program")
    private val testCompilationFactory = TestCompilationFactory()
    private val objcCacheArgs = TestCompilerArgs(listOf("-Xbinary=objcExportCache=true"))

    private fun getObjCCacheDir(cache: TestCompilationArtifact.KLIBStaticCache): File {
        val base = cache.cacheDir
        val objcDir = File(base.absolutePath + ".objc")
        return if (objcDir.exists()) objcDir else base
    }

    private fun compileStdlibCache(): TestCompilationArtifact.KLIBStaticCache {
        val stdlibPath = testRunSettings.get<KotlinNativeHome>().dir.resolve("klib/common/stdlib")
        val stdlibKlib = TestCompilationArtifact.KLIB(stdlibPath)
        val cacheDir = buildDir.resolve("cacheStdlib").apply { mkdirs() }
        return compileToStaticCache(
            stdlibKlib,
            cacheDir,
            freeCompilerArgs = objcCacheArgs
        )
    }

    @Test
    @TestMetadata("interfaceImpl")
    fun testInterfaceImplementationAcrossCaches() {
        val testName = "interfaceImpl"
        val testDir = testSuiteDir.resolve(testName)

        val cacheStdlib = compileStdlibCache()

        val libA = compileToLibrary(
            testDir.resolve("libA"),
            buildDir.resolve("libA"),
            TestCompilerArgs("-module-name", "libA"),
            emptyList(),
        )
        val cacheA = compileToStaticCache(
            libA,
            buildDir.resolve("cacheA").apply { mkdirs() },
            cacheStdlib,
            freeCompilerArgs = TestCompilerArgs(
                listOf(
                    "-Xbinary=objcExportCache=true",
                    "-Xcache-directory=${getObjCCacheDir(cacheStdlib).absolutePath}"
                )
            )
        )

        val libB = compileToLibrary(
            testDir.resolve("libB"),
            buildDir.resolve("libB"),
            TestCompilerArgs("-module-name", "libB"),
            listOf(libA.asLibraryDependency()),
        )
        val cacheB = compileToStaticCache(
            libB,
            buildDir.resolve("cacheB").apply { mkdirs() },
            cacheStdlib, cacheA,
            freeCompilerArgs = TestCompilerArgs(
                listOf(
                    "-Xbinary=objcExportCache=true",
                    "-Xcache-directory=${getObjCCacheDir(cacheStdlib).absolutePath}",
                    "-Xcache-directory=${getObjCCacheDir(cacheA).absolutePath}"
                )
            )
        )

        runObjCExportCacheFrameworkTest(testName, testDir, listOf(libA, libB), listOf(cacheStdlib, cacheA, cacheB))
    }

    @Test
    @TestMetadata("categoryExt")
    fun testCategoryExtensionFunctionOnCachedClass() {
        val testName = "categoryExt"
        val testDir = testSuiteDir.resolve(testName)

        val cacheStdlib = compileStdlibCache()

        val libA = compileToLibrary(
            testDir.resolve("libA"),
            buildDir.resolve("libA"),
            TestCompilerArgs("-module-name", "libA"),
            emptyList(),
        )
        val cacheA = compileToStaticCache(
            libA,
            buildDir.resolve("cacheA").apply { mkdirs() },
            cacheStdlib,
            freeCompilerArgs = TestCompilerArgs(
                listOf(
                    "-Xbinary=objcExportCache=true",
                    "-Xcache-directory=${getObjCCacheDir(cacheStdlib).absolutePath}"
                )
            )
        )

        val libB = compileToLibrary(
            testDir.resolve("libB"),
            buildDir.resolve("libB"),
            TestCompilerArgs("-module-name", "libB"),
            listOf(libA.asLibraryDependency()),
        )
        val cacheB = compileToStaticCache(
            libB,
            buildDir.resolve("cacheB").apply { mkdirs() },
            cacheStdlib, cacheA,
            freeCompilerArgs = TestCompilerArgs(
                listOf(
                    "-Xbinary=objcExportCache=true",
                    "-Xcache-directory=${getObjCCacheDir(cacheStdlib).absolutePath}",
                    "-Xcache-directory=${getObjCCacheDir(cacheA).absolutePath}"
                )
            )
        )

        runObjCExportCacheFrameworkTest(testName, testDir, listOf(libA, libB), listOf(cacheStdlib, cacheA, cacheB))
    }

    @Test
    @TestMetadata("fileClassCollision")
    fun testFileClassSymbolCollisionAcrossCaches() {
        val testName = "fileClassCollision"
        val testDir = testSuiteDir.resolve(testName)

        val cacheStdlib = compileStdlibCache()

        val libA = compileToLibrary(
            testDir.resolve("libA"),
            buildDir.resolve("libA"),
            TestCompilerArgs("-module-name", "libA"),
            emptyList(),
        )
        val cacheA = compileToStaticCache(
            libA,
            buildDir.resolve("cacheA").apply { mkdirs() },
            cacheStdlib,
            freeCompilerArgs = TestCompilerArgs(
                listOf(
                    "-Xbinary=objcExportCache=true",
                    "-Xcache-directory=${getObjCCacheDir(cacheStdlib).absolutePath}"
                )
            )
        )

        val libB = compileToLibrary(
            testDir.resolve("libB"),
            buildDir.resolve("libB"),
            TestCompilerArgs("-module-name", "libB"),
            emptyList(),
        )
        val cacheB = compileToStaticCache(
            libB,
            buildDir.resolve("cacheB").apply { mkdirs() },
            cacheStdlib,
            freeCompilerArgs = TestCompilerArgs(
                listOf(
                    "-Xbinary=objcExportCache=true",
                    "-Xcache-directory=${getObjCCacheDir(cacheStdlib).absolutePath}"
                )
            )
        )

        runObjCExportCacheFrameworkTest(testName, testDir, listOf(libA, libB), listOf(cacheStdlib, cacheA, cacheB))
    }

    @Test
    @TestMetadata("inheritedMethod")
    fun testSubclassingAcrossCachesWithInheritedMethod() {
        val testName = "inheritedMethod"
        val testDir = testSuiteDir.resolve(testName)

        val cacheStdlib = compileStdlibCache()

        val libA = compileToLibrary(
            testDir.resolve("libA"),
            buildDir.resolve("libA"),
            TestCompilerArgs("-module-name", "libA"),
            emptyList(),
        )
        val cacheA = compileToStaticCache(
            libA,
            buildDir.resolve("cacheA").apply { mkdirs() },
            cacheStdlib,
            freeCompilerArgs = TestCompilerArgs(
                listOf(
                    "-Xbinary=objcExportCache=true",
                    "-Xcache-directory=${getObjCCacheDir(cacheStdlib).absolutePath}"
                )
            )
        )

        val libB = compileToLibrary(
            testDir.resolve("libB"),
            buildDir.resolve("libB"),
            TestCompilerArgs("-module-name", "libB"),
            listOf(libA.asLibraryDependency()),
        )
        val cacheB = compileToStaticCache(
            libB,
            buildDir.resolve("cacheB").apply { mkdirs() },
            cacheStdlib, cacheA,
            freeCompilerArgs = TestCompilerArgs(
                listOf(
                    "-Xbinary=objcExportCache=true",
                    "-Xcache-directory=${getObjCCacheDir(cacheStdlib).absolutePath}",
                    "-Xcache-directory=${getObjCCacheDir(cacheA).absolutePath}"
                )
            )
        )

        runObjCExportCacheFrameworkTest(testName, testDir, listOf(libA, libB), listOf(cacheStdlib, cacheA, cacheB))
    }

    @Test
    @TestMetadata("multiLevel")
    fun testMultiLevelCacheChain() {
        val testName = "multiLevel"
        val testDir = testSuiteDir.resolve(testName)

        val cacheStdlib = compileStdlibCache()

        val libA = compileToLibrary(
            testDir.resolve("libA"),
            buildDir.resolve("libA"),
            TestCompilerArgs("-module-name", "libA"),
            emptyList(),
        )
        val cacheA = compileToStaticCache(
            libA,
            buildDir.resolve("cacheA").apply { mkdirs() },
            cacheStdlib,
            freeCompilerArgs = TestCompilerArgs(
                listOf(
                    "-Xbinary=objcExportCache=true",
                    "-Xcache-directory=${getObjCCacheDir(cacheStdlib).absolutePath}"
                )
            )
        )

        val libB = compileToLibrary(
            testDir.resolve("libB"),
            buildDir.resolve("libB"),
            TestCompilerArgs("-module-name", "libB"),
            listOf(libA.asLibraryDependency()),
        )
        val cacheB = compileToStaticCache(
            libB,
            buildDir.resolve("cacheB").apply { mkdirs() },
            cacheStdlib, cacheA,
            freeCompilerArgs = TestCompilerArgs(
                listOf(
                    "-Xbinary=objcExportCache=true",
                    "-Xcache-directory=${getObjCCacheDir(cacheStdlib).absolutePath}",
                    "-Xcache-directory=${getObjCCacheDir(cacheA).absolutePath}"
                )
            )
        )

        val libC = compileToLibrary(
            testDir.resolve("libC"),
            buildDir.resolve("build_libC"),
            TestCompilerArgs("-module-name", "libC"),
            listOf(libA.asLibraryDependency(), libB.asLibraryDependency()),
        )
        val cacheC = compileToStaticCache(
            libC,
            buildDir.resolve("cacheC").apply { mkdirs() },
            cacheStdlib, cacheA, cacheB,
            freeCompilerArgs = TestCompilerArgs(
                listOf(
                    "-Xbinary=objcExportCache=true",
                    "-Xcache-directory=${getObjCCacheDir(cacheStdlib).absolutePath}",
                    "-Xcache-directory=${getObjCCacheDir(cacheA).absolutePath}",
                    "-Xcache-directory=${getObjCCacheDir(cacheB).absolutePath}"
                )
            )
        )

        runObjCExportCacheFrameworkTest(testName, testDir, listOf(libA, libB, libC), listOf(cacheStdlib, cacheA, cacheB, cacheC))
    }

    private fun runObjCExportCacheFrameworkTest(
        testName: String,
        testDir: File,
        klibs: List<TestCompilationArtifact.KLIB>,
        caches: List<TestCompilationArtifact.KLIBStaticCache>
    ) {
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        Assumptions.assumeFalse(testRunSettings.get<CacheMode>() == CacheMode.WithoutCache)

        val frameworkName = "Kt"
        val frameworkOpts = listOf(
            "-Xstatic-framework",
            "-Xbinary=objcExportCache=true",
            "-Xbinary=bundleId=$frameworkName",
            "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
            "-module-name", frameworkName
        ) + klibs.map { "-Xexport-library=${it.klibFile.absolutePath}" } + caches.flatMap {
            val dir = getObjCCacheDir(it)
            listOf("-Xcache-directory=${it.cacheDir.absolutePath}", "-Xcache-directory=${dir.absolutePath}")
        }

        val dummyFile = buildDir.resolve("dummy_${testName}.kt").also {
            if (!it.exists()) it.writeText("package com.example\n\n// dummy file for framework compilation")
        }
        val testCase = generateObjCFrameworkTestCase(
            TestKind.STANDALONE_NO_TR, extras, frameworkName,
            listOf(dummyFile),
            freeCompilerArgs = TestCompilerArgs(frameworkOpts),
            givenDependencies = klibs.map { TestModule.Given(it.klibFile) }.toSet(),
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout * 2),
        )

        val success = testCompilationFactory.testCaseToObjCFrameworkCompilation(
            testCase, testRunSettings, exportedLibraries = klibs
        ).result.assertSuccess()
        val frameworkArtifact = success.resultingArtifact

        compileAndRunSwift(testName, testCase, testDir, frameworkArtifact)
        compileAndRunObjC(testName, testCase, testDir, frameworkArtifact)
    }

    private fun compileAndRunSwift(
        testName: String,
        testCase: TestCase,
        testDir: File,
        frameworkArtifact: TestCompilationArtifact.ObjCFramework,
    ) {
        val swiftSources = testDir.listFiles { file: File -> file.name.endsWith(".swift") }?.toList().orEmpty()
        if (swiftSources.isEmpty()) return

        val swiftCompilation = SwiftCompilation(
            testRunSettings,
            swiftSources,
            TestCompilationArtifact.Executable(buildDir.resolve("${testName}_swiftExecutable")),
            listOf(
                "-Xlinker", "-rpath", "-Xlinker", "@executable_path/Frameworks",
                "-Xlinker", "-rpath", "-Xlinker", buildDir.absolutePath,
                "-F", buildDir.absolutePath
            ),
            outputFile = { executable -> executable.executableFile }
        ).result.assertSuccess()

        val testExecutable = TestExecutable(
            swiftCompilation.resultingArtifact,
            swiftCompilation.loggedData,
            listOf(TestName(testName))
        )
        runExecutableAndVerify(testCase, testExecutable)
    }

    private fun compileAndRunObjC(
        testName: String,
        testCase: TestCase,
        testDir: File,
        frameworkArtifact: TestCompilationArtifact.ObjCFramework,
    ) {
        val objcSources = testDir.listFiles { file: File -> file.name.endsWith(".m") }?.toList().orEmpty()
        if (objcSources.isEmpty()) return

        val executableArtifact = TestCompilationArtifact.Executable(buildDir.resolve("${testName}_objcExecutable"))
        val clangResult = compileWithClang(
            clangMode = ClangMode.C,
            sourceFiles = objcSources,
            outputFile = executableArtifact.executableFile,
            frameworkDirectories = listOf(buildDir),
            additionalClangFlags = listOf(
                "-fobjc-arc",
                "-F", buildDir.absolutePath,
                "-framework", frameworkArtifact.frameworkName,
                "-Wl,-rpath,${buildDir.absolutePath}"
            )
        ).assertSuccess()

        val testExecutable = TestExecutable(
            clangResult.resultingArtifact,
            clangResult.loggedData,
            listOf(TestName(testName))
        )
        runExecutableAndVerify(testCase, testExecutable)
    }

    companion object {
        const val TEST_SUITE_PATH = "native/native.tests/testData/framework/objcExportCache"
    }
}
