/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.nativeBinaryOptions.GC
import org.jetbrains.kotlin.config.nativeBinaryOptions.GCSchedulerType
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.isMacabi
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.*
import org.jetbrains.kotlin.konan.test.blackbox.support.util.createTestProvider
import org.jetbrains.kotlin.test.TestDataAssertions.assertEqualsToFile
import org.jetbrains.kotlin.test.KtAssert.fail
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertFalse
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.time.Duration

@TestDataPath("\$PROJECT_ROOT")
class FrameworkTest : AbstractNativeSimpleTest() {
    private val testSuiteDir = ForTestCompileRuntime.transformTestDataPath("native/native.tests/testData/framework")
    private val extras = TestCase.NoTestRunnerExtras("There's no entrypoint in Swift program")
    private val testCompilationFactory = TestCompilationFactory()

    @Test
    fun testKT65659() {
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        val testDataFile = testSuiteDir.resolve("kt65659.kt")
        val testCase = generateObjCFrameworkTestCase(
            TestKind.STANDALONE_NO_TR,
            extras,
            "kt65659",
            listOf(testDataFile),
            TestCompilerArgs(listOf("-Xbinary=bundleId=kt65659")),
        )
        val objCFrameworkCompilation = testCompilationFactory.testCaseToObjCFrameworkCompilation(testCase, testRunSettings)
        val compilationResult = objCFrameworkCompilation.result.assertSuccess()
        assertTrue(compilationResult.resultingArtifact.mainHeader.readText().contains("aliasedAndReturnError"))
    }

    @Test
    fun testSignextZeroext() {
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        val fileCheckStage = "CStubs"
        val testDataFile = testSuiteDir.resolve("signext_zeroext_objc_export.kt")
        val testCase = generateObjCFrameworkTestCase(
            TestKind.STANDALONE_NO_TR,
            extras,
            "SignextZeroext",
            listOf(testDataFile),
            TestCompilerArgs(
                listOf(
                    "-Xbinary=bundleId=signextZeroext",
                    "-Xsave-llvm-ir-after=$fileCheckStage",
                    "-Xsave-llvm-ir-directory=${buildDir.absolutePath}",
                )
            ),
            givenDependencies = emptySet(),
            // KT-64879: TODO: refactor fileCheckMatcher out from TestRunChecks to another layer like TestExecutableChecks
            checks = TestRunChecks.Default(Duration.ZERO)
                .copy(fileCheckMatcher = TestRunCheck.FileCheckMatcher(testRunSettings, testDataFile))
        )
        val objCFrameworkCompilation = testCompilationFactory.testCaseToObjCFrameworkCompilation(testCase, testRunSettings)
        objCFrameworkCompilation.result.assertSuccess()

        val fileCheckDump = buildDir.resolve("out.$fileCheckStage.ll").also { assert(it.exists()) }
        val result = testCase.checks.fileCheckMatcher!!.doFileCheck(fileCheckDump)
        if (!(result.stdout.isEmpty() && result.stderr.isEmpty())) {
            val shortOutText = result.stdout.lines().take(100)
            val shortErrText = result.stderr.lines().take(100)
            fail("FileCheck matching of ${fileCheckDump.absolutePath}\n" +
                         "with '--check-prefixes ${testCase.checks.fileCheckMatcher.prefixes}'\n" +
                         "failed with result=$result:\n" +
                         shortOutText.joinToString("\n") + "\n" +
                         shortErrText.joinToString("\n")
            )
        }
    }

    @Test
    fun testValuesGenerics() {
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        val testName = "values_generics"

        val testCase = generateObjCFrameworkTestCase(
            TestKind.STANDALONE_NO_TR, extras, "ValuesGenerics",
            listOf(
                testSuiteDir.resolve(testName).resolve("$testName.kt"),
                testSuiteDir.resolve("objcexport/values.kt"),
            ),
            freeCompilerArgs = TestCompilerArgs(listOf("-opt-in=kotlinx.cinterop.ExperimentalForeignApi"))
        )
        testCompilationFactory.testCaseToObjCFrameworkCompilation(testCase, testRunSettings).result.assertSuccess()

        compileAndRunSwift(testName, testCase)
    }

    @Test
    fun testStdlib() {
        val testName = "stdlib"
        val testCase = generateObjCFramework(testName, testCompilerArgs = listOf("-Xdisable-ir-checkers=IrVisibilityChecker"))
        compileAndRunSwift(testName, testCase)
    }

    @Test
    fun testMultipleFrameworks() {
        // This test might fail with dynamic caches until https://youtrack.jetbrains.com/issue/KT-34262 is fixed
        val checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout)
        testMultipleFrameworksImpl("multiple", emptyList(), checks)
    }

    @Test
    fun testMultipleFrameworksStatic() {
        // https://youtrack.jetbrains.com/issue/KT-67572
        Assumptions.assumeFalse(testRunSettings.get<ThreadStateChecker>() == ThreadStateChecker.ENABLED)

        val checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout)
        testMultipleFrameworksImpl("multiple", listOf("-Xstatic-framework", "-Xpre-link-caches=enable"), checks)
    }

    @Test
    fun testMultipleFrameworksStaticFailsWithStaticCaches() {
        // https://youtrack.jetbrains.com/issue/KT-67572
        Assumptions.assumeFalse(testRunSettings.get<ThreadStateChecker>() == ThreadStateChecker.ENABLED)

        val defaultChecks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout)
        val checks = if (testRunSettings.get<CacheMode>() != CacheMode.WithoutCache) {
            // KT-34261: two asserts in testIsolation4() fail with static caches.
            defaultChecks.copy(exitCodeCheck = TestRunCheck.ExitCode.Expected(134))
        } else defaultChecks

        testMultipleFrameworksImpl("multipleFailsWithCaches", listOf("-Xstatic-framework", "-Xpre-link-caches=enable"), checks)
    }

    private fun testMultipleFrameworksImpl(testName: String, freeCompilerArgs: List<String>, checks: TestRunChecks) {
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)

        val testDir = testSuiteDir.resolve("multiple")
        val framework1Dir = testDir.resolve("framework1")
        val sharedDir = testDir.resolve("shared")
        val moduleNameFirst = "First"
        val testCase1 = generateObjCFrameworkTestCase(
            TestKind.STANDALONE_NO_TR, extras, moduleNameFirst,
            listOf(
                framework1Dir.resolve("first.kt"),
                framework1Dir.resolve("test.kt"),
                sharedDir.resolve("shared.kt"),
            ),
            freeCompilerArgs = TestCompilerArgs(
                freeCompilerArgs + "-module-name" + moduleNameFirst + "-Xbinary=bundleId=$moduleNameFirst"
            ),
            checks = checks,
        )
        testCompilationFactory.testCaseToObjCFrameworkCompilation(testCase1, testRunSettings).result.assertSuccess()

        val framework2Dir = testDir.resolve("framework2")
        val moduleNameSecond = "Second"
        val testCase2 = generateObjCFrameworkTestCase(
            TestKind.STANDALONE_NO_TR, extras, moduleNameSecond,
            listOf(
                framework2Dir.resolve("second.kt"),
                framework2Dir.resolve("test.kt"),
                sharedDir.resolve("shared.kt"),
            ), freeCompilerArgs = TestCompilerArgs(
                freeCompilerArgs + "-module-name" + moduleNameSecond + "-Xbinary=bundleId=$moduleNameSecond"
            )
        )
        testCompilationFactory.testCaseToObjCFrameworkCompilation(testCase2, testRunSettings).result.assertSuccess()

        compileAndRunSwift(testName, testCase1, swiftExtraOpts = emptyList(), testDir)
    }

    @Test
    fun testGH3343() {
        val testName = "gh3343"
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        val freeCInteropArgs = TestCompilerArgs(emptyList(), cinteropArgs = listOf("-header", "$testName.h"))
        val interopLibrary = compileCInterop(testName, freeCInteropArgs)
        val testCase = generateObjCFramework(testName, emptyList(), setOf(TestModule.Given(interopLibrary.klibFile)))
        compileAndRunSwift(testName, testCase)
    }

    @Test
    fun testKT43517() {
        val testName = "kt43517"
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        val interopLibrary = compileCInterop(testName)

        val testCase = generateObjCFramework(testName, emptyList(), setOf(TestModule.Given(interopLibrary.klibFile)))
        compileAndRunSwift(testName, testCase)
    }

    @Test
    fun testKT66565_usingModuleMapSyntaxInKotlinModuleNameMakesImportableModule() {
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        val reservedModuleMapSyntax = "umbrella"
        val testName = "kt66565"
        generateObjCFramework(testName, moduleName = reservedModuleMapSyntax)
        SwiftCompilation(
            testRunSettings,
            listOf(testSuiteDir.resolve(testName).resolve("$testName.swift")),
            TestCompilationArtifact.BinaryLibrary(buildDir.resolve("swiftObject")),
            listOf(
                "-c",
                "-F", buildDir.absolutePath
            ),
            outputFile = { library -> library.libraryFile }
        ).result.assertSuccess()
    }

    @Test
    fun testStacktrace() {
        val testName = "stacktrace"
        Assumptions.assumeFalse(testRunSettings.get<OptimizationMode>() == OptimizationMode.OPT)
        // Stacktraces support for Mac Catalyst requires additional adjustments in `supportsCoreSymbolication`.
        // We can do it later if needed.
        Assumptions.assumeFalse(testRunSettings.configurables.targetTriple.isMacabi)

        val testCase = generateObjCFramework(testName, listOf("-g"))
        compileAndRunSwift(testName, testCase)
    }

    @Test
    fun testStacktraceBridges() {
        val testName = "stacktraceBridges"
        Assumptions.assumeFalse(testRunSettings.get<OptimizationMode>() == OptimizationMode.OPT)

        val testCase = generateObjCFramework(testName, listOf("-g"))
        compileAndRunSwift(testName, testCase)
    }

    @Test
    fun testStacktraceByLibbacktrace() {
        Assumptions.assumeFalse(testRunSettings.get<OptimizationMode>() == OptimizationMode.OPT)
        val testName = "stacktraceByLibbacktrace"
        val testCase = generateObjCFramework(testName, listOf("-g", "-Xbinary=sourceInfoType=libbacktrace"))
        compileAndRunSwift(testName, testCase)
    }

    @Test
    fun testAbstractInstantiation() {
        val testName = "abstractInstantiation"
        val checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout).copy(
            exitCodeCheck = TestRunCheck.ExitCode.Expected(134)
        )
        val testCase = generateObjCFramework(testName, checks = checks)
        compileAndRunSwift(testName, testCase)
    }

    @Test
    fun testFrameworkBundleId() {
        Assumptions.assumeTrue(testRunSettings.get<KotlinNativeTargets>().testTarget.family == Family.OSX)
        val testName = "bundle_id"
        val testDir = testSuiteDir.resolve(testName)
        val freeCompilerArgs = TestCompilerArgs(
            listOf(
                "-Xbinary=bundleId=$testName",
                "-Xbinary=bundleVersion=FooBundleVersion",
                "-Xbinary=bundleShortVersionString=FooBundleShortVersionString"
            )
        )
        val testCase = generateObjCFrameworkTestCase(
            TestKind.STANDALONE_NO_TR, extras, testName,
            listOf(
                testDir.resolve("main.kt"),
                testDir.resolve("lib.kt"),
            ),
            freeCompilerArgs
        )
        testCompilationFactory.testCaseToObjCFrameworkCompilation(testCase, testRunSettings).result.assertSuccess()

        val buildDir = testRunSettings.get<Binaries>().testBinariesDir
        val infoPlist = buildDir.resolve("$testName.framework/Resources/Info.plist")
        val infoPlistContents = infoPlist.readText()
        listOf(
            "<key>CFBundleIdentifier</key>\\s*<string>$testName</string>",
            "<key>CFBundleShortVersionString</key>\\s*<string>FooBundleShortVersionString</string>",
            "<key>CFBundleVersion</key>\\s*<string>FooBundleVersion</string>",
        ).forEach {
            assertTrue(infoPlistContents.contains(Regex(it))) {
                "${infoPlist.absolutePath} does not contain pattern `$it`:\n$infoPlistContents"
            }
        }
    }

    @Test
    fun testSanitizedBundleId() {
        Assumptions.assumeTrue(testRunSettings.get<KotlinNativeTargets>().testTarget.family == Family.OSX)
        val testName = "sanitizedBundleId"
        val moduleName = "S@nitizedВundle Id" // NOTE: uses cyrillic В.
        val testCase = generateObjCFrameworkTestCase(
            TestKind.STANDALONE_NO_TR,
            extras,
            moduleName,
            listOf(testSuiteDir.resolve(testName).resolve("lib.kt")),
        )
        val objCFrameworkCompilation = testCompilationFactory.testCaseToObjCFrameworkCompilation(testCase, testRunSettings).result.assertSuccess()

        val expectedBundleId = "sp-aces.da-sh-es.S-nitized-undle-Id"
        val buildDir = testRunSettings.get<Binaries>().testBinariesDir
        val infoPlist = buildDir.resolve("$moduleName.framework/Resources/Info.plist")
        val infoPlistContents = infoPlist.readText()
        listOf(
            "<key>CFBundleIdentifier</key>\\s*<string>$expectedBundleId</string>",
        ).forEach {
            assertTrue(infoPlistContents.contains(Regex(it))) {
                "${infoPlist.absolutePath} does not contain pattern `$it`:\n$infoPlistContents"
            }
        }
    }

    @Test
    fun testForwardDeclarations() {
        val testName = "forwardDeclarations"
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        val interopLibrary = compileCInterop(testName)

        val testCase = generateObjCFramework(testName, emptyList(), setOf(TestModule.Given(interopLibrary.klibFile)))
        compileAndRunSwift(testName, testCase)
    }

    private fun compileCInterop(testName: String, freeCInteropArgs: TestCompilerArgs = TestCompilerArgs.EMPTY) =
        cinteropToLibrary(
            defFile = testSuiteDir.resolve(testName).resolve("$testName.def"),
            outputDir = buildDir,
            freeCompilerArgs = freeCInteropArgs
        ).assertSuccess().resultingArtifact

    @Test
    fun testUseFoundationModule() {
        val testName = "use_foundation_module"
        generateObjCFramework(testName)
        val modulemap = buildDir.resolve("$testName.framework/Modules/module.modulemap")
        val modulemapContents = modulemap.readText()
        val expectedPattern = "use Foundation"
        assertTrue(modulemapContents.contains(expectedPattern)) {
            "${modulemap.absolutePath} must contain `$expectedPattern`:\n$modulemapContents"
        }
    }

    @Test
    fun testKT56233() {
        val testName = "kt56233"
        // test must make huge amount of repetitions to make sure there's no race conditions, so bigger timeout is needed. Double is not enough
        val checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout * 10)
        val testCase = generateObjCFramework(testName, checks = checks)
        val swiftExtraOpts = if (testRunSettings.get<GCScheduler>().scheduler != GCSchedulerType.AGGRESSIVE) listOf() else
            listOf("-D", "AGGRESSIVE_GC")
        compileAndRunSwift(testName, testCase, swiftExtraOpts)
    }

    @Test
    fun testKT57791() {
        val testName = "kt57791"
        val testCase = generateObjCFramework(testName)
        compileAndRunSwift(testName, testCase)
    }

    @Test
    fun testKT78837() {
        val testName = "kt78837"
        val testCase = generateObjCFramework(testName)
        compileAndRunSwift(testName, testCase)
    }

    @Test
    fun testPermanentObjects() {
        val testName = "permanentObjects"
        Assumptions.assumeFalse(testRunSettings.get<GCType>().gc == GC.NOOP) { "Test requires GC to actually happen" }

        val testCase = generateObjCFramework(testName, listOf("-opt-in=kotlin.native.internal.InternalForKotlinNative"))
        compileAndRunSwift(testName, testCase)
    }

    @Test
    fun testReflection() {
        val testName = "reflection"
        val testCase = generateObjCFramework(testName, listOf("-opt-in=kotlin.native.internal.InternalForKotlinNative"))
        compileAndRunSwift(testName, testCase)
    }

    @Test
    fun testLatin1Disabled() {
        val testName = "latin1"
        val testCase = generateObjCFramework(testName, listOf("-Xbinary=latin1Strings=false"))
        compileAndRunSwift(testName, testCase)
    }

    @Test
    fun testLatin1Enabled() {
        val testName = "latin1"
        val testCase = generateObjCFramework(testName, listOf("-Xbinary=latin1Strings=true"))
        compileAndRunSwift(testName, testCase, swiftExtraOpts=listOf("-D", "ENABLE_LATIN1"))
    }

    @Test
    fun testBlockParamNames() {
        val testName = "blockParamNames"
        val testCase = generateObjCFramework(testName)
        compileAndRunSwift(testName, testCase)
    }

    @Test
    fun objCExportTest() {
        objCExportTestImpl("", emptyList(), emptyList(), false)
    }

    @Test
    fun objCExportTestNoGenerics() {
        objCExportTestImpl("NoGenerics", listOf("-Xno-objc-generics"),
                           listOf("-D", "NO_GENERICS"), false)
    }

    @Test
    fun objCExportTestLegacySuspendUnit() {
        objCExportTestImpl("LegacySuspendUnit", listOf("-Xbinary=unitSuspendFunctionObjCExport=legacy"),
                           listOf("-D", "LEGACY_SUSPEND_UNIT_FUNCTION_EXPORT"), false)
    }

    @Test
    fun objCExportTestNoSwiftMemberNameMangling() {
        objCExportTestImpl("NoSwiftMemberNameMangling", listOf("-Xbinary=objcExportDisableSwiftMemberNameMangling=true"),
                           listOf("-D", "DISABLE_MEMBER_NAME_MANGLING"), false)
    }

    @Test
    fun objCExportTestNoInterfaceMemberNameMangling() {
        objCExportTestImpl("NoInterfaceMemberNameMangling", listOf("-Xbinary=objcExportIgnoreInterfaceMethodCollisions=true"),
                           listOf("-D", "DISABLE_INTERFACE_METHOD_NAME_MANGLING"), false)
    }

    @Test
    fun objCExportTestStatic() {
        objCExportTestImpl("Static", listOf("-Xbinary=objcExportSuspendFunctionLaunchThreadRestriction=main"),
                           listOf("-D", "DISALLOW_SUSPEND_ANY_THREAD"), true)
    }

    private fun produceStaticCache(klibFile: File, cacheDir: File, extraArgs: List<String> = emptyList()) {
        val konanHome = testRunSettings.get<KotlinNativeHome>().dir
        val distCacheDir = konanHome.resolve("klib/cache").listFiles { f -> f.name.startsWith(targets.testTarget.name) }?.firstOrNull { it.resolve("stdlib-cache").exists() }
            ?: konanHome.resolve("klib/cache").listFiles { f -> f.name.startsWith(targets.testTarget.name) }?.firstOrNull { it.resolve("stdlib-per-file-cache").exists() }
        val kotlinc = konanHome.resolve("bin").resolve("kotlinc-native")
        val staticCacheArgs = buildList {
            add("-produce")
            add("static_cache")
            add("-Xadd-cache=${klibFile.absolutePath}")
            add("-Xcache-directory=${cacheDir.absolutePath}")
            if (distCacheDir != null) {
                add("-Xcache-directory=${distCacheDir.absolutePath}")
            }
            add("-target")
            add(targets.testTarget.name)
            addAll(extraArgs)
        }
        org.jetbrains.kotlin.native.executors.runProcess(
            kotlinc.absolutePath,
            *staticCacheArgs.toTypedArray()
        ) {
            environment["JAVA_HOME"] = System.getProperty("java.home")
        }
    }

    private fun produceObjCCache(
        klibFile: File,
        frameworkName: String,
        cacheDir: File,
        extraArgs: List<String> = emptyList(),
    ) {
        val konanHome = testRunSettings.get<KotlinNativeHome>().dir
        val distCacheDir = konanHome.resolve("klib/cache").listFiles { f -> f.name.startsWith(targets.testTarget.name) }?.firstOrNull { it.resolve("stdlib-cache").exists() }
            ?: konanHome.resolve("klib/cache").listFiles { f -> f.name.startsWith(targets.testTarget.name) }?.firstOrNull { it.resolve("stdlib-per-file-cache").exists() }
        val kotlinc = konanHome.resolve("bin").resolve("kotlinc-native")
        val objcCacheArgs = buildList {
            add("-produce")
            add("objc_cache")
            add("-Xadd-cache=${klibFile.absolutePath}")
            add("-module-name")
            add(frameworkName)
            add("-Xcache-directory=${cacheDir.absolutePath}")
            if (distCacheDir != null) {
                add("-Xcache-directory=${distCacheDir.absolutePath}")
            }
            add("-target")
            add(targets.testTarget.name)
            addAll(extraArgs)
        }
        org.jetbrains.kotlin.native.executors.runProcess(
            kotlinc.absolutePath,
            *objcCacheArgs.toTypedArray()
        ) {
            environment["JAVA_HOME"] = System.getProperty("java.home")
        }
    }

    @Test
    fun testObjCCacheCompilationAndFrameworkLink() {
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        val library = compileToLibrary(
            testSuiteDir.resolve("objcexport/library"),
            buildDir,
            TestCompilerArgs("-Xshort-module-name=MyLibrary", "-module-name", "org.jetbrains.kotlin.native.test-library"),
            emptyList(),
        )
        val cacheDir = buildDir.resolve("cache").apply { mkdirs() }
        val frameworkName = "Kt"
        produceStaticCache(library.klibFile, cacheDir)
        produceObjCCache(library.klibFile, frameworkName, cacheDir)

        val ktFiles = testSuiteDir.resolve("objcexport").listFiles { file: File -> file.name.endsWith(".kt") }!!.toList()
        val testCase = generateObjCFrameworkTestCase(
            TestKind.STANDALONE_NO_TR, extras, frameworkName,
            ktFiles,
            freeCompilerArgs = TestCompilerArgs(
                listOf(
                    "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                    "-Xbinary=bundleId=foo.bar",
                    "-module-name", frameworkName,
                    "-Xexport-library=${library.klibFile.absolutePath}",
                    "-Xcache-directory=${cacheDir.absolutePath}",
                    "-Xdisable-ir-checkers=IrVisibilityChecker",
                )
            ),
            givenDependencies = setOf(TestModule.Given(library.klibFile)),
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout * 5),
        )
        testCompilationFactory.testCaseToObjCFrameworkCompilation(testCase, testRunSettings).result.assertSuccess()

        val testSwiftFile = buildDir.resolve("objcCacheTest.swift").apply {
            writeText(
                """
                import Kt

                func testAccessClassFromObjCCache() throws {
                    let object = A(data: "Data from Class")
                    let enumObject = E.b

                    try assertEquals(actual: object.data, expected: "Data from Class")
                    try assertEquals(actual: enumObject.data, expected: "Enum entry B")
                }

                class ObjcCacheTestTests : SimpleTestProvider {
                    override init() {
                        super.init()
                        test("testAccessClassFromObjCCache", testAccessClassFromObjCCache)
                    }
                }
                """.trimIndent()
            )
        }
        val swiftFiles = testSuiteDir.resolve("objcexport").listFiles { file: File -> file.name.endsWith(".swift") && file.name != "library.swift" }!!.toList() + listOf(testSwiftFile)
        val swiftExtraOpts = buildList {
            if (testRunSettings.get<GCScheduler>().scheduler == GCSchedulerType.AGGRESSIVE) {
                add("-D")
                add("AGGRESSIVE_GC")
            }
            if (testRunSettings.get<GCType>().gc == GC.NOOP) {
                add("-D")
                add("NOOP_GC")
            }
        }
        val successExecutable = compileSwift(swiftFiles, swiftExtraOpts)
        val testExecutable = TestExecutable(
            successExecutable.resultingArtifact,
            successExecutable.loggedData,
            listOf(TestName("testObjCCacheCompilationAndFrameworkLink"))
        )
        runExecutableAndVerify(testCase, testExecutable)
    }

    @Test
    fun testObjCCacheStaticFrameworkLink() {
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        val library = compileToLibrary(
            testSuiteDir.resolve("objcexport/library"),
            buildDir,
            TestCompilerArgs("-Xshort-module-name=MyLibrary", "-module-name", "org.jetbrains.kotlin.native.test-library"),
            emptyList(),
        )
        val cacheDir = buildDir.resolve("cache_static").apply { mkdirs() }
        val frameworkName = "Kt"
        produceStaticCache(library.klibFile, cacheDir)
        produceObjCCache(library.klibFile, frameworkName, cacheDir)

        val testCase = generateObjCFrameworkTestCase(
            TestKind.STANDALONE_NO_TR, extras, frameworkName,
            listOf(testSuiteDir.resolve("objcexport/library.kt")),
            freeCompilerArgs = TestCompilerArgs(
                listOf(
                    "-Xstatic-framework",
                    "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                    "-Xbinary=bundleId=foo.bar",
                    "-module-name", frameworkName,
                    "-Xexport-library=${library.klibFile.absolutePath}",
                    "-Xcache-directory=${cacheDir.absolutePath}",
                    "-Xdisable-ir-checkers=IrVisibilityChecker",
                )
            ),
            givenDependencies = setOf(TestModule.Given(library.klibFile)),
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout * 5),
        )
        val success = testCompilationFactory.testCaseToObjCFrameworkCompilation(testCase, testRunSettings).result.assertSuccess()
        val frameworkBinary = success.resultingArtifact.frameworkDir.let {
            it.resolve("Versions/A/$frameworkName").takeIf { f -> f.exists() } ?: it.resolve(frameworkName)
        }
        assertTrue(frameworkBinary.exists()) { "Framework binary $frameworkBinary does not exist" }

        val testSwiftFile = buildDir.resolve("staticObjcCacheTest.swift").apply {
            writeText(
                """
                import Kt

                func testAccessClassFromObjCCache() throws {
                    let object = A(data: "Data from Static Cache")
                    let enumObject = E.b

                    try assertEquals(actual: object.data, expected: "Data from Static Cache")
                    try assertEquals(actual: enumObject.data, expected: "Enum entry B")
                }

                class StaticObjcCacheTestTests : SimpleTestProvider {
                    override init() {
                        super.init()
                        test("testAccessClassFromObjCCache", testAccessClassFromObjCCache)
                    }
                }
                """.trimIndent()
            )
        }
        val successExecutable = compileSwift(listOf(testSwiftFile), emptyList())
        val testExecutable = TestExecutable(
            successExecutable.resultingArtifact,
            successExecutable.loggedData,
            listOf(TestName("testObjCCacheStaticFrameworkLink"))
        )
        runExecutableAndVerify(testCase, testExecutable)
    }

    @Test
    fun testObjCCacheStaticFrameworkMultipleLibraries() {
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        val libASrc = buildDir.resolve("libA_src").apply {
            mkdirs()
            resolve("libA.kt").writeText(
                """
                package liba
                class Alpha(val name: String)
                """.trimIndent()
            )
        }
        val libBSrc = buildDir.resolve("libB_src").apply {
            mkdirs()
            resolve("libB.kt").writeText(
                """
                package libb
                import liba.Alpha
                class Beta(val alpha: Alpha, val count: Int)
                """.trimIndent()
            )
        }
        val libA = compileToLibrary(
            libASrc,
            buildDir.resolve("libA_out"),
            TestCompilerArgs("-module-name", "libA"),
            emptyList(),
        )
        val libB = compileToLibrary(
            libBSrc,
            buildDir.resolve("libB_out"),
            TestCompilerArgs("-module-name", "libB"),
            listOf(libA.asLibraryDependency()),
        )
        val cacheDir = buildDir.resolve("cache_multiple_static").apply { mkdirs() }
        val frameworkName = "Kt"
        val libBDeps = listOf("-l", libA.klibFile.absolutePath)
        produceStaticCache(libA.klibFile, cacheDir)
        produceStaticCache(libB.klibFile, cacheDir, libBDeps)
        produceObjCCache(libA.klibFile, frameworkName, cacheDir)
        produceObjCCache(libB.klibFile, frameworkName, cacheDir, libBDeps)

        val frameworkSrc = buildDir.resolve("framework_src").apply {
            mkdirs()
            resolve("framework.kt").writeText("package test\nfun frameworkMarker() = 1")
        }
        val testCase = generateObjCFrameworkTestCase(
            TestKind.STANDALONE_NO_TR, extras, frameworkName,
            listOf(frameworkSrc.resolve("framework.kt")),
            freeCompilerArgs = TestCompilerArgs(
                listOf(
                    "-Xstatic-framework",
                    "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                    "-Xbinary=bundleId=foo.bar",
                    "-module-name", frameworkName,
                    "-Xexport-library=${libA.klibFile.absolutePath}",
                    "-Xexport-library=${libB.klibFile.absolutePath}",
                    "-Xcache-directory=${cacheDir.absolutePath}",
                    "-Xdisable-ir-checkers=IrVisibilityChecker",
                )
            ),
            givenDependencies = setOf(TestModule.Given(libA.klibFile), TestModule.Given(libB.klibFile)),
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout * 5),
        )
        testCompilationFactory.testCaseToObjCFrameworkCompilation(testCase, testRunSettings).result.assertSuccess()

        val testSwiftFile = buildDir.resolve("multipleObjcCacheTest.swift").apply {
            writeText(
                """
                import Kt

                func testMultipleObjCCaches() throws {
                    let alpha = Alpha(name: "AlphaCached")
                    let beta = Beta(alpha: alpha, count: 42)

                    try assertEquals(actual: beta.alpha.name, expected: "AlphaCached")
                    try assertEquals(actual: beta.count, expected: 42)
                }

                class MultipleObjcCacheTestTests : SimpleTestProvider {
                    override init() {
                        super.init()
                        test("testMultipleObjCCaches", testMultipleObjCCaches)
                    }
                }
                """.trimIndent()
            )
        }
        val successExecutable = compileSwift(listOf(testSwiftFile), emptyList())
        val testExecutable = TestExecutable(
            successExecutable.resultingArtifact,
            successExecutable.loggedData,
            listOf(TestName("testObjCCacheStaticFrameworkMultipleLibraries"))
        )
        runExecutableAndVerify(testCase, testExecutable)
    }

    @Test
    fun testObjCCacheStaticFrameworkTransitiveDependencyNotExported() {
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        val libASrc = buildDir.resolve("libA_transitive_src").apply {
            mkdirs()
            resolve("libA.kt").writeText(
                """
                package liba
                class Alpha(val name: String)
                """.trimIndent()
            )
        }
        val libBSrc = buildDir.resolve("libB_transitive_src").apply {
            mkdirs()
            resolve("libB.kt").writeText(
                """
                package libb
                import liba.Alpha
                class Beta(val alpha: Alpha, val count: Int)
                """.trimIndent()
            )
        }
        val libA = compileToLibrary(
            libASrc,
            buildDir.resolve("libA_transitive_out"),
            TestCompilerArgs("-module-name", "libA"),
            emptyList(),
        )
        val libB = compileToLibrary(
            libBSrc,
            buildDir.resolve("libB_transitive_out"),
            TestCompilerArgs("-module-name", "libB"),
            listOf(libA.asLibraryDependency()),
        )
        val cacheDir = buildDir.resolve("cache_transitive_static").apply { mkdirs() }
        val frameworkName = "Kt"
        val libBDeps = listOf("-l", libA.klibFile.absolutePath)
        produceStaticCache(libA.klibFile, cacheDir)
        produceStaticCache(libB.klibFile, cacheDir, libBDeps)
        produceObjCCache(libA.klibFile, frameworkName, cacheDir)
        produceObjCCache(libB.klibFile, frameworkName, cacheDir, libBDeps)

        val frameworkSrc = buildDir.resolve("framework_transitive_src").apply {
            mkdirs()
            resolve("framework.kt").writeText("package test\nfun frameworkMarker() = 1")
        }
        val testCase = generateObjCFrameworkTestCase(
            TestKind.STANDALONE_NO_TR, extras, frameworkName,
            listOf(frameworkSrc.resolve("framework.kt")),
            freeCompilerArgs = TestCompilerArgs(
                listOf(
                    "-Xstatic-framework",
                    "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                    "-Xbinary=bundleId=foo.bar",
                    "-module-name", frameworkName,
                    // Note: only libB is exported, libA is a transitive dependency!
                    "-Xexport-library=${libB.klibFile.absolutePath}",
                    "-Xcache-directory=${cacheDir.absolutePath}",
                    "-Xdisable-ir-checkers=IrVisibilityChecker",
                )
            ),
            givenDependencies = setOf(TestModule.Given(libA.klibFile), TestModule.Given(libB.klibFile)),
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout * 5),
        )
        testCompilationFactory.testCaseToObjCFrameworkCompilation(testCase, testRunSettings).result.assertSuccess()

        val testSwiftFile = buildDir.resolve("transitiveObjcCacheTest.swift").apply {
            writeText(
                """
                import Kt

                func testTransitiveObjCCache() throws {
                    let alpha = LibAAlpha(name: "AlphaCached")
                    let beta = Beta(alpha: alpha, count: 42)

                    try assertEquals(actual: beta.alpha.name, expected: "AlphaCached")
                    try assertEquals(actual: beta.count, expected: 42)
                }

                class TransitiveObjcCacheTestTests : SimpleTestProvider {
                    override init() {
                        super.init()
                        test("testTransitiveObjCCache", testTransitiveObjCCache)
                    }
                }
                """.trimIndent()
            )
        }
        val successExecutable = compileSwift(listOf(testSwiftFile), emptyList())
        val testExecutable = TestExecutable(
            successExecutable.resultingArtifact,
            successExecutable.loggedData,
            listOf(TestName("testObjCCacheStaticFrameworkTransitiveDependencyNotExported"))
        )
        runExecutableAndVerify(testCase, testExecutable)
    }

    @Test
    fun testObjCCacheStaticFrameworkMixedCaching() {
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        val libASrc = buildDir.resolve("libA_mixed_src").apply {
            mkdirs()
            resolve("libA.kt").writeText(
                """
                package liba
                class Alpha(val name: String)
                """.trimIndent()
            )
        }
        val libBSrc = buildDir.resolve("libB_mixed_src").apply {
            mkdirs()
            resolve("libB.kt").writeText(
                """
                package libb
                import liba.Alpha
                class Beta(val alpha: Alpha, val count: Int)
                """.trimIndent()
            )
        }
        val libA = compileToLibrary(
            libASrc,
            buildDir.resolve("libA_mixed_out"),
            TestCompilerArgs("-module-name", "libA"),
            emptyList(),
        )
        val libB = compileToLibrary(
            libBSrc,
            buildDir.resolve("libB_mixed_out"),
            TestCompilerArgs("-module-name", "libB"),
            listOf(libA.asLibraryDependency()),
        )
        val cacheDir = buildDir.resolve("cache_mixed_static").apply { mkdirs() }
        val frameworkName = "Kt"
        // Precompile cache ONLY for libA; libB is uncached
        produceStaticCache(libA.klibFile, cacheDir)
        produceObjCCache(libA.klibFile, frameworkName, cacheDir)

        val frameworkSrc = buildDir.resolve("framework_mixed_src").apply {
            mkdirs()
            resolve("framework.kt").writeText("package test\nfun frameworkMarker() = 1")
        }
        val testCase = generateObjCFrameworkTestCase(
            TestKind.STANDALONE_NO_TR, extras, frameworkName,
            listOf(frameworkSrc.resolve("framework.kt")),
            freeCompilerArgs = TestCompilerArgs(
                listOf(
                    "-Xstatic-framework",
                    "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                    "-Xbinary=bundleId=foo.bar",
                    "-module-name", frameworkName,
                    "-Xexport-library=${libA.klibFile.absolutePath}",
                    "-Xexport-library=${libB.klibFile.absolutePath}",
                    "-Xcache-directory=${cacheDir.absolutePath}",
                    "-Xdisable-ir-checkers=IrVisibilityChecker",
                )
            ),
            givenDependencies = setOf(TestModule.Given(libA.klibFile), TestModule.Given(libB.klibFile)),
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout * 5),
        )
        testCompilationFactory.testCaseToObjCFrameworkCompilation(testCase, testRunSettings).result.assertSuccess()

        val testSwiftFile = buildDir.resolve("mixedObjcCacheTest.swift").apply {
            writeText(
                """
                import Kt

                func testMixedObjCCache() throws {
                    let alpha = Alpha(name: "AlphaCached")
                    let beta = Beta(alpha: alpha, count: 99)

                    try assertEquals(actual: beta.alpha.name, expected: "AlphaCached")
                    try assertEquals(actual: beta.count, expected: 99)
                }

                class MixedObjcCacheTestTests : SimpleTestProvider {
                    override init() {
                        super.init()
                        test("testMixedObjCCache", testMixedObjCCache)
                    }
                }
                """.trimIndent()
            )
        }
        val successExecutable = compileSwift(listOf(testSwiftFile), emptyList())
        val testExecutable = TestExecutable(
            successExecutable.resultingArtifact,
            successExecutable.loggedData,
            listOf(TestName("testObjCCacheStaticFrameworkMixedCaching"))
        )
        runExecutableAndVerify(testCase, testExecutable)
    }

    @Test
    fun testObjCCacheWithObjCExportEntryPoints() {
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        val library = compileToLibrary(
            testSuiteDir.resolve("objcexport/library"),
            buildDir,
            TestCompilerArgs("-Xshort-module-name=MyLibrary", "-module-name", "org.jetbrains.kotlin.native.test-library"),
            emptyList(),
        )
        val cacheDir = buildDir.resolve("cache_entry_points").apply { mkdirs() }
        val frameworkName = "Kt"
        val entryPointsFile = buildDir.resolve("entrypoints.txt").apply {
            writeText(
                """
                callable library.A.data
                callable library.A.<init>
                """.trimIndent()
            )
        }
        produceStaticCache(library.klibFile, cacheDir)
        produceObjCCache(library.klibFile, frameworkName, cacheDir, listOf("-Xbinary=objcExportEntryPointsPath=${entryPointsFile.absolutePath}"))

        val ktFiles = testSuiteDir.resolve("objcexport").listFiles { file: File -> file.name.endsWith(".kt") }!!.toList()
        val testCase = generateObjCFrameworkTestCase(
            TestKind.STANDALONE_NO_TR, extras, frameworkName,
            ktFiles,
            freeCompilerArgs = TestCompilerArgs(
                listOf(
                    "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                    "-Xbinary=bundleId=foo.bar",
                    "-module-name", frameworkName,
                    "-Xexport-library=${library.klibFile.absolutePath}",
                    "-Xcache-directory=${cacheDir.absolutePath}",
                    "-Xbinary=objcExportEntryPointsPath=${entryPointsFile.absolutePath}",
                    "-Xdisable-ir-checkers=IrVisibilityChecker",
                )
            ),
            givenDependencies = setOf(TestModule.Given(library.klibFile)),
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout * 5),
        )
        val success = testCompilationFactory.testCaseToObjCFrameworkCompilation(testCase, testRunSettings).result.assertSuccess()
        val headerFile = success.resultingArtifact.frameworkDir.resolve("Headers/$frameworkName.h")
        assertTrue(headerFile.exists()) { "Header file $headerFile does not exist" }
        val headerContent = headerFile.readText()
        assertTrue(headerContent.contains("initWithData:")) { "Expected initWithData: in header:\n$headerContent" }
        assertTrue(headerContent.contains("@property (readonly) NSString *data")) { "Expected data property in header:\n$headerContent" }
        assertFalse(headerContent.contains("readDataFromLibraryEnum")) { "Expected readDataFromLibraryEnum to be excluded from header:\n$headerContent" }
    }

    @Test
    fun testObjCCacheStaticFrameworkCrossLibraryCategory() {
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        val libASrc = buildDir.resolve("libA_cross_src").apply {
            mkdirs()
            resolve("libA.kt").writeText(
                """
                package liba
                class Alpha(val name: String)
                """.trimIndent()
            )
        }
        val libBSrc = buildDir.resolve("libB_cross_src").apply {
            mkdirs()
            resolve("libB.kt").writeText(
                """
                package libb
                import liba.Alpha
                fun Alpha.greet(): String = "Hello " + this.name
                """.trimIndent()
            )
        }
        val libA = compileToLibrary(
            libASrc,
            buildDir.resolve("libA_cross_out"),
            TestCompilerArgs("-module-name", "libA"),
            emptyList(),
        )
        val libB = compileToLibrary(
            libBSrc,
            buildDir.resolve("libB_cross_out"),
            TestCompilerArgs("-module-name", "libB"),
            listOf(libA.asLibraryDependency()),
        )
        val cacheDir = buildDir.resolve("cache_cross_static").apply { mkdirs() }
        val frameworkName = "Kt"
        val libBDeps = listOf("-l", libA.klibFile.absolutePath)
        produceStaticCache(libA.klibFile, cacheDir)
        produceStaticCache(libB.klibFile, cacheDir, libBDeps)
        produceObjCCache(libA.klibFile, frameworkName, cacheDir)
        produceObjCCache(libB.klibFile, frameworkName, cacheDir, libBDeps)

        val frameworkSrc = buildDir.resolve("framework_cross_src").apply {
            mkdirs()
            resolve("framework.kt").writeText("package test\nfun frameworkMarker() = 1")
        }
        val testCase = generateObjCFrameworkTestCase(
            TestKind.STANDALONE_NO_TR, extras, frameworkName,
            listOf(frameworkSrc.resolve("framework.kt")),
            freeCompilerArgs = TestCompilerArgs(
                listOf(
                    "-Xstatic-framework",
                    "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                    "-Xbinary=bundleId=foo.bar",
                    "-module-name", frameworkName,
                    "-Xexport-library=${libA.klibFile.absolutePath}",
                    "-Xexport-library=${libB.klibFile.absolutePath}",
                    "-Xcache-directory=${cacheDir.absolutePath}",
                    "-Xdisable-ir-checkers=IrVisibilityChecker",
                )
            ),
            givenDependencies = setOf(TestModule.Given(libA.klibFile), TestModule.Given(libB.klibFile)),
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout * 5),
        )
        testCompilationFactory.testCaseToObjCFrameworkCompilation(testCase, testRunSettings).result.assertSuccess()

        val testSwiftFile = buildDir.resolve("crossCategoryObjcCacheTest.swift").apply {
            writeText(
                """
                import Kt

                func testCrossLibraryCategory() throws {
                    let alpha = Alpha(name: "AlphaCached")
                    try assertEquals(actual: alpha.greet(), expected: "Hello AlphaCached")
                }

                class CrossCategoryObjcCacheTestTests : SimpleTestProvider {
                    override init() {
                        super.init()
                        test("testCrossLibraryCategory", testCrossLibraryCategory)
                    }
                }
                """.trimIndent()
            )
        }
        val successExecutable = compileSwift(listOf(testSwiftFile), emptyList())
        val testExecutable = TestExecutable(
            successExecutable.resultingArtifact,
            successExecutable.loggedData,
            listOf(TestName("testObjCCacheStaticFrameworkCrossLibraryCategory"))
        )
        runExecutableAndVerify(testCase, testExecutable)
    }

    @Test
    fun testObjCCacheArtifactAndMetadataLayout() {
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        val libSrc = buildDir.resolve("layout_lib_src").apply {
            mkdirs()
            resolve("lib.kt").writeText(
                """
                package test.layout
                interface LayoutProtocol {
                    fun doWork(): String
                }
                class LayoutWorker : LayoutProtocol {
                    override fun doWork(): String = "done"
                }
                """.trimIndent()
            )
        }
        val library = compileToLibrary(
            libSrc,
            buildDir.resolve("layout_lib_out"),
            TestCompilerArgs("-module-name", "layoutLib"),
            emptyList(),
        )
        val cacheDir = buildDir.resolve("cache_layout").apply { mkdirs() }
        val frameworkName = "Kt"
        produceStaticCache(library.klibFile, cacheDir)
        produceObjCCache(library.klibFile, frameworkName, cacheDir)

        val objcCacheDir = cacheDir.resolve("layoutLib-$frameworkName.objc_cache")
        assertTrue(objcCacheDir.exists()) { "Expected objc_cache directory $objcCacheDir to exist" }

        val binDir = objcCacheDir.resolve("bin")
        assertTrue(binDir.exists()) { "Expected bin directory $binDir to exist" }

        val archiveFiles = binDir.listFiles { file: File -> file.name.endsWith(".a") }
        assertTrue(!archiveFiles.isNullOrEmpty()) { "Expected static archive .a file in $binDir" }

        val metadataFile = binDir.resolve("objc_cache_metadata.properties")
        assertTrue(metadataFile.exists()) { "Expected metadata file $metadataFile to exist" }

        val properties = java.util.Properties().apply {
            metadataFile.bufferedReader().use { load(it) }
        }

        val targetName = properties.getProperty("targetName")
        assertTrue(!targetName.isNullOrEmpty()) { "Metadata targetName should not be empty" }
        assertTrue(targetName == targets.testTarget.name) {
            "Expected targetName ${targets.testTarget.name}, but was $targetName"
        }

        val klibHash = properties.getProperty("klibHash")
        assertTrue(!klibHash.isNullOrEmpty()) { "Metadata klibHash should not be empty" }

        val compilerFingerprint = properties.getProperty("compilerFingerprint")
        assertTrue(compilerFingerprint != null) { "Metadata should contain compilerFingerprint property" }

        val classAdapters = properties.getProperty("classAdapters")
        assertTrue(classAdapters != null) { "Metadata must contain classAdapters property" }
        assertTrue(classAdapters.contains("LayoutWorker")) {
            "Expected classAdapters to contain LayoutWorker, but was $classAdapters"
        }

        val protocolAdapters = properties.getProperty("protocolAdapters")
        assertTrue(protocolAdapters != null) { "Metadata must contain protocolAdapters property" }
        assertTrue(protocolAdapters.contains("LayoutProtocol")) {
            "Expected protocolAdapters to contain LayoutProtocol, but was $protocolAdapters"
        }
    }

    @Test
    fun testObjCCacheDynamicFrameworkCrossLibraryCategory() {
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        val libASrc = buildDir.resolve("libA_cross_dyn_src").apply {
            mkdirs()
            resolve("libA.kt").writeText(
                """
                package liba
                class Alpha(val name: String)
                """.trimIndent()
            )
        }
        val libBSrc = buildDir.resolve("libB_cross_dyn_src").apply {
            mkdirs()
            resolve("libB.kt").writeText(
                """
                package libb
                import liba.Alpha
                fun Alpha.greet(): String = "Hello " + this.name
                """.trimIndent()
            )
        }
        val libA = compileToLibrary(
            libASrc,
            buildDir.resolve("libA_cross_dyn_out"),
            TestCompilerArgs("-module-name", "libA"),
            emptyList(),
        )
        val libB = compileToLibrary(
            libBSrc,
            buildDir.resolve("libB_cross_dyn_out"),
            TestCompilerArgs("-module-name", "libB"),
            listOf(libA.asLibraryDependency()),
        )
        val cacheDir = buildDir.resolve("cache_cross_dynamic").apply { mkdirs() }
        val frameworkName = "Kt"
        val libBDeps = listOf("-l", libA.klibFile.absolutePath)
        produceStaticCache(libA.klibFile, cacheDir)
        produceStaticCache(libB.klibFile, cacheDir, libBDeps)
        produceObjCCache(libA.klibFile, frameworkName, cacheDir)
        produceObjCCache(libB.klibFile, frameworkName, cacheDir, libBDeps)

        val frameworkSrc = buildDir.resolve("framework_cross_dyn_src").apply {
            mkdirs()
            resolve("framework.kt").writeText("package test\nfun frameworkMarker() = 1")
        }
        val testCase = generateObjCFrameworkTestCase(
            TestKind.STANDALONE_NO_TR, extras, frameworkName,
            listOf(frameworkSrc.resolve("framework.kt")),
            freeCompilerArgs = TestCompilerArgs(
                listOf(
                    "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                    "-Xbinary=bundleId=foo.bar",
                    "-module-name", frameworkName,
                    "-Xexport-library=${libA.klibFile.absolutePath}",
                    "-Xexport-library=${libB.klibFile.absolutePath}",
                    "-Xcache-directory=${cacheDir.absolutePath}",
                    "-Xdisable-ir-checkers=IrVisibilityChecker",
                )
            ),
            givenDependencies = setOf(TestModule.Given(libA.klibFile), TestModule.Given(libB.klibFile)),
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout * 5),
        )
        testCompilationFactory.testCaseToObjCFrameworkCompilation(testCase, testRunSettings).result.assertSuccess()

        val testSwiftFile = buildDir.resolve("crossCategoryDynamicObjcCacheTest.swift").apply {
            writeText(
                """
                import Kt

                func testCrossLibraryCategoryDynamic() throws {
                    let alpha = Alpha(name: "AlphaDynamic")
                    try assertEquals(actual: alpha.greet(), expected: "Hello AlphaDynamic")
                }

                class CrossCategoryDynamicObjcCacheTestTests : SimpleTestProvider {
                    override init() {
                        super.init()
                        test("testCrossLibraryCategoryDynamic", testCrossLibraryCategoryDynamic)
                    }
                }
                """.trimIndent()
            )
        }
        val successExecutable = compileSwift(listOf(testSwiftFile), emptyList())
        val testExecutable = TestExecutable(
            successExecutable.resultingArtifact,
            successExecutable.loggedData,
            listOf(TestName("testObjCCacheDynamicFrameworkCrossLibraryCategory"))
        )
        runExecutableAndVerify(testCase, testExecutable)
    }

    @Test
    fun testObjCCacheDynamicFrameworkTransitiveDependencyNotExported() {
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        val libASrc = buildDir.resolve("libA_transitive_dyn_src").apply {
            mkdirs()
            resolve("libA.kt").writeText(
                """
                package liba
                class Alpha(val name: String)
                """.trimIndent()
            )
        }
        val libBSrc = buildDir.resolve("libB_transitive_dyn_src").apply {
            mkdirs()
            resolve("libB.kt").writeText(
                """
                package libb
                import liba.Alpha
                class Beta(val alpha: Alpha, val count: Int)
                """.trimIndent()
            )
        }
        val libA = compileToLibrary(
            libASrc,
            buildDir.resolve("libA_transitive_dyn_out"),
            TestCompilerArgs("-module-name", "libA"),
            emptyList(),
        )
        val libB = compileToLibrary(
            libBSrc,
            buildDir.resolve("libB_transitive_dyn_out"),
            TestCompilerArgs("-module-name", "libB"),
            listOf(libA.asLibraryDependency()),
        )
        val cacheDir = buildDir.resolve("cache_transitive_dynamic").apply { mkdirs() }
        val frameworkName = "Kt"
        val libBDeps = listOf("-l", libA.klibFile.absolutePath)
        produceStaticCache(libA.klibFile, cacheDir)
        produceStaticCache(libB.klibFile, cacheDir, libBDeps)
        produceObjCCache(libA.klibFile, frameworkName, cacheDir)
        produceObjCCache(libB.klibFile, frameworkName, cacheDir, libBDeps)

        val frameworkSrc = buildDir.resolve("framework_transitive_dyn_src").apply {
            mkdirs()
            resolve("framework.kt").writeText("package test\nfun frameworkMarker() = 1")
        }
        val testCase = generateObjCFrameworkTestCase(
            TestKind.STANDALONE_NO_TR, extras, frameworkName,
            listOf(frameworkSrc.resolve("framework.kt")),
            freeCompilerArgs = TestCompilerArgs(
                listOf(
                    "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                    "-Xbinary=bundleId=foo.bar",
                    "-module-name", frameworkName,
                    // Note: only libB is exported, libA is a transitive dependency in dynamic framework!
                    "-Xexport-library=${libB.klibFile.absolutePath}",
                    "-Xcache-directory=${cacheDir.absolutePath}",
                    "-Xdisable-ir-checkers=IrVisibilityChecker",
                )
            ),
            givenDependencies = setOf(TestModule.Given(libA.klibFile), TestModule.Given(libB.klibFile)),
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout * 5),
        )
        testCompilationFactory.testCaseToObjCFrameworkCompilation(testCase, testRunSettings).result.assertSuccess()

        val testSwiftFile = buildDir.resolve("transitiveDynamicObjcCacheTest.swift").apply {
            writeText(
                """
                import Kt

                func testTransitiveObjCCacheDynamic() throws {
                    let alpha = LibAAlpha(name: "AlphaDynamicCached")
                    let beta = Beta(alpha: alpha, count: 42)

                    try assertEquals(actual: beta.alpha.name, expected: "AlphaDynamicCached")
                    try assertEquals(actual: beta.count, expected: 42)
                }

                class TransitiveDynamicObjcCacheTestTests : SimpleTestProvider {
                    override init() {
                        super.init()
                        test("testTransitiveObjCCacheDynamic", testTransitiveObjCCacheDynamic)
                    }
                }
                """.trimIndent()
            )
        }
        val successExecutable = compileSwift(listOf(testSwiftFile), emptyList())
        val testExecutable = TestExecutable(
            successExecutable.resultingArtifact,
            successExecutable.loggedData,
            listOf(TestName("testObjCCacheDynamicFrameworkTransitiveDependencyNotExported"))
        )
        runExecutableAndVerify(testCase, testExecutable)
    }

    @Test
    fun testObjCCacheRuntimeParityWithCleanFramework() {
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        val paritySrc = buildDir.resolve("parity_src").apply {
            mkdirs()
            resolve("parity.kt").writeText(
                """
                package parity

                open class ParityPerson(val name: String, var age: Int) {
                    open fun describe(): String = "Person(name=" + name + ", age=" + age + ")"
                }

                class ParityStudent(name: String, age: Int, val university: String) : ParityPerson(name, age) {
                    override fun describe(): String = "Student(name=" + name + ", age=" + age + ", university=" + university + ")"
                }

                enum class ParityDirection(val degrees: Int) {
                    NORTH(0),
                    EAST(90),
                    SOUTH(180),
                    WEST(270);

                    fun opposite(): ParityDirection = when (this) {
                        NORTH -> SOUTH
                        EAST -> WEST
                        SOUTH -> NORTH
                        WEST -> EAST
                    }
                }

                class ParityMathOps {
                    companion object {
                        fun add(a: Int, b: Int): Int = a + b
                    }
                }

                class ParityCalculator {
                    @Throws(Throwable::class)
                    fun divide(a: Int, b: Int): Int {
                        if (b == 0) throw IllegalArgumentException("Division by zero")
                        return a / b
                    }
                }

                fun ParityPerson.celebrateBirthday(): String {
                    age += 1
                    return "Happy birthday " + name + ", now " + age
                }

                fun parityMultiply(a: Int, b: Int): Int = a * b
                """.trimIndent()
            )
        }
        val parityLib = compileToLibrary(
            paritySrc,
            buildDir.resolve("parity_lib_out"),
            TestCompilerArgs("-module-name", "parityLib"),
            emptyList(),
        )

        fun createSwiftTestCode(moduleName: String, className: String): String =
            """
            import $moduleName

            func runParityAssertions() throws {
                let person = ParityPerson(name: "Ada", age: 25)
                try assertEquals(actual: person.name, expected: "Ada")
                try assertEquals(actual: person.age, expected: 25)
                try assertEquals(actual: person.describe(), expected: "Person(name=Ada, age=25)")

                let student = ParityStudent(name: "Grace", age: 19, university: "ComputingCollege")
                try assertEquals(actual: student.name, expected: "Grace")
                try assertEquals(actual: student.age, expected: 19)
                try assertEquals(actual: student.university, expected: "ComputingCollege")
                try assertEquals(actual: student.describe(), expected: "Student(name=Grace, age=19, university=ComputingCollege)")

                person.age = 26
                try assertEquals(actual: person.age, expected: 26)

                let birthdayMsg = person.celebrateBirthday()
                try assertEquals(actual: birthdayMsg, expected: "Happy birthday Ada, now 27")
                try assertEquals(actual: person.age, expected: 27)

                let product = ParityKt.parityMultiply(a: 6, b: 7)
                try assertEquals(actual: product, expected: 42)

                let dir = ParityDirection.north
                try assertEquals(actual: dir.degrees, expected: 0)
                try assertEquals(actual: dir.opposite(), expected: ParityDirection.south)

                let sum = ParityMathOps.companion.add(a: 20, b: 22)
                try assertEquals(actual: sum, expected: 42)

                let calc = ParityCalculator()
                var exceptionThrown = false
                do {
                    _ = try calc.divide(a: 10, b: 0)
                } catch {
                    exceptionThrown = true
                }
                try assertTrue(exceptionThrown)
            }

            class $className : SimpleTestProvider {
                override init() {
                    super.init()
                    test("runParityAssertions", runParityAssertions)
                }
            }
            """.trimIndent()

        // 1. Clean Framework
        val cleanFrameworkName = "KtClean"
        val cleanMarkerFile = buildDir.resolve("clean_marker.kt").apply {
            writeText("package test\nfun cleanMarker() = 1")
        }
        val cleanTestCase = generateObjCFrameworkTestCase(
            TestKind.STANDALONE_NO_TR, extras, cleanFrameworkName,
            listOf(cleanMarkerFile),
            freeCompilerArgs = TestCompilerArgs(
                listOf(
                    "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                    "-Xbinary=bundleId=parity.clean",
                    "-module-name", cleanFrameworkName,
                    "-Xexport-library=${parityLib.klibFile.absolutePath}",
                    "-Xdisable-ir-checkers=IrVisibilityChecker",
                )
            ),
            givenDependencies = setOf(TestModule.Given(parityLib.klibFile)),
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout * 5),
        )
        val cleanResult = testCompilationFactory.testCaseToObjCFrameworkCompilation(cleanTestCase, testRunSettings).result.assertSuccess()

        val cleanSwiftFile = buildDir.resolve("cleanParity.swift").apply {
            writeText(createSwiftTestCode(cleanFrameworkName, "CleanParityTests"))
        }
        val cleanExecutable = compileSwift(listOf(cleanSwiftFile), emptyList())
        val cleanTestExec = TestExecutable(
            cleanExecutable.resultingArtifact,
            cleanExecutable.loggedData,
            listOf(TestName("testObjCCacheRuntimeParityClean"))
        )
        runExecutableAndVerify(cleanTestCase, cleanTestExec)

        // 2. Cached Framework
        val cachedFrameworkName = "KtCached"
        val cacheDir = buildDir.resolve("cache_parity").apply { mkdirs() }
        produceStaticCache(parityLib.klibFile, cacheDir)
        produceObjCCache(parityLib.klibFile, cachedFrameworkName, cacheDir)

        val cachedMarkerFile = buildDir.resolve("cached_marker.kt").apply {
            writeText("package test\nfun cachedMarker() = 1")
        }
        val cachedTestCase = generateObjCFrameworkTestCase(
            TestKind.STANDALONE_NO_TR, extras, cachedFrameworkName,
            listOf(cachedMarkerFile),
            freeCompilerArgs = TestCompilerArgs(
                listOf(
                    "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                    "-Xbinary=bundleId=parity.cached",
                    "-module-name", cachedFrameworkName,
                    "-Xexport-library=${parityLib.klibFile.absolutePath}",
                    "-Xcache-directory=${cacheDir.absolutePath}",
                    "-Xdisable-ir-checkers=IrVisibilityChecker",
                )
            ),
            givenDependencies = setOf(TestModule.Given(parityLib.klibFile)),
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout * 5),
        )
        val cachedResult = testCompilationFactory.testCaseToObjCFrameworkCompilation(cachedTestCase, testRunSettings).result.assertSuccess()

        val cachedSwiftFile = buildDir.resolve("cachedParity.swift").apply {
            writeText(createSwiftTestCode(cachedFrameworkName, "CachedParityTests"))
        }
        val cachedExecutable = compileSwift(listOf(cachedSwiftFile), emptyList())
        val cachedTestExec = TestExecutable(
            cachedExecutable.resultingArtifact,
            cachedExecutable.loggedData,
            listOf(TestName("testObjCCacheRuntimeParityCached"))
        )
        runExecutableAndVerify(cachedTestCase, cachedTestExec)

        // 3. Header Parity Comparison
        val cleanHeader = cleanResult.resultingArtifact.mainHeader.readText()
        val cachedHeader = cachedResult.resultingArtifact.mainHeader.readText()

        assertTrue(cleanHeader.contains("@interface ${cleanFrameworkName}ParityPerson")) { "Clean header missing ParityPerson" }
        assertTrue(cachedHeader.contains("@interface ${cachedFrameworkName}ParityPerson")) { "Cached header missing ParityPerson" }

        assertTrue(cleanHeader.contains("@interface ${cleanFrameworkName}ParityStudent : ${cleanFrameworkName}ParityPerson")) { "Clean header missing ParityStudent" }
        assertTrue(cachedHeader.contains("@interface ${cachedFrameworkName}ParityStudent : ${cachedFrameworkName}ParityPerson")) { "Cached header missing ParityStudent" }

        assertTrue(cleanHeader.contains("@interface ${cleanFrameworkName}ParityDirection")) { "Clean header missing ParityDirection" }
        assertTrue(cachedHeader.contains("@interface ${cachedFrameworkName}ParityDirection")) { "Cached header missing ParityDirection" }

        assertTrue(cleanHeader.contains("@interface ${cleanFrameworkName}ParityMathOps")) { "Clean header missing ParityMathOps" }
        assertTrue(cachedHeader.contains("@interface ${cachedFrameworkName}ParityMathOps")) { "Cached header missing ParityMathOps" }

        assertTrue(cleanHeader.contains("celebrateBirthday")) { "Clean header missing celebrateBirthday" }
        assertTrue(cachedHeader.contains("celebrateBirthday")) { "Cached header missing celebrateBirthday" }

        assertTrue(cleanHeader.contains("parityMultiply")) { "Clean header missing parityMultiply" }
        assertTrue(cachedHeader.contains("parityMultiply")) { "Cached header missing parityMultiply" }
    }

    @Test
    fun objCExportDumpObjcSelectorToSignatureMapping() {
        Assumptions.assumeTrue(testRunSettings.get<KotlinNativeTargets>().testTarget.family == Family.OSX)
        val testName = "selectorToSignatureDump"
        val testDir = testSuiteDir.resolve(testName)
        val dumpFile = buildDir.resolve("dump.txt")
        val goldenFile = testDir.resolve("golden.txt")
        val freeCompilerArgs = TestCompilerArgs(
            listOf(
                "-module-name", testName,
                "-Xbinary=bundleId=$testName",
                "-Xbinary=bundleVersion=FooBundleVersion",
                "-Xbinary=bundleShortVersionString=FooBundleShortVersionString",
                "-Xbinary=dumpObjcSelectorToSignatureMapping=${dumpFile.absolutePath}",
                "-Xomit-framework-binary"
            )
        )
        val testCase = generateObjCFrameworkTestCase(
            TestKind.STANDALONE_NO_TR, extras, testName,
            listOf(
                testDir.resolve("main.kt"),
            ),
            freeCompilerArgs
        )
        testCompilationFactory.testCaseToObjCFrameworkCompilation(testCase, testRunSettings).result.assertSuccess()

        fun File.parseDump(): List<Set<String>> =
            readText().split("\n\n").map { it.lines().drop(1).toSet() }

        val dump = dumpFile.parseDump()
        val golden = goldenFile.parseDump()
        if (dump != golden) {
            // The following assert will fail here, and provide better UX than asserting that dump is equal to golden
            assertEqualsToFile(goldenFile, dumpFile.readText())
        }
    }


    private fun objCExportTestImpl(
        suffix: String,
        frameworkOpts: List<String>,
        swiftOpts: List<String>,
        isStaticFramework: Boolean,
    ) {
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)

        // Compile a couple of KLIBs
        val library = compileToLibrary(
            testSuiteDir.resolve("objcexport/library"),
            buildDir,
            TestCompilerArgs("-Xshort-module-name=MyLibrary", "-module-name", "org.jetbrains.kotlin.native.test-library"),
            emptyList(),
        )

        // Convert KT sources into ObjC framework using two KLIbs
        val objcExportTestSuiteDir = testSuiteDir.resolve("objcexport")
        val ktFiles = objcExportTestSuiteDir.listFiles { file: File -> file.name.endsWith(".kt") }
        assertTrue(ktFiles != null && ktFiles.isNotEmpty()) {
            "Some .kt files must be in test folder $objcExportTestSuiteDir"
        }
        val frameworkName = "Kt"
        val testCase = generateObjCFrameworkTestCase(
            TestKind.STANDALONE_NO_TR, extras, "Kt",
            ktFiles!!.toList(),
            freeCompilerArgs = TestCompilerArgs(
                frameworkOpts + listOfNotNull(
                    "-Xstatic-framework".takeIf { isStaticFramework },
                    "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                    "-Xexport-kdoc",
                    "-Xbinary=bundleId=foo.bar",
                    "-module-name", frameworkName,
                    "-Xdisable-ir-checkers=IrVisibilityChecker",
                )
            ),
            givenDependencies = setOf(TestModule.Given(library.klibFile)),
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout * 5), // objcexport is a test suite on its own, increase the default timeout
        )
        testCompilationFactory.testCaseToObjCFrameworkCompilation(testCase, testRunSettings).result.assertSuccess()

        // compile Swift sources using generated ObjC framework
        val swiftFiles = objcExportTestSuiteDir.listFiles { file: File -> file.name.endsWith(".swift") }
        assertTrue(swiftFiles != null && swiftFiles.isNotEmpty()) {
            "Some .swift files must be in test folder $objcExportTestSuiteDir"
        }
        val swiftExtraOpts = buildList {
            addAll(swiftOpts)
            if (testRunSettings.get<GCScheduler>().scheduler == GCSchedulerType.AGGRESSIVE) {
                add("-D")
                add("AGGRESSIVE_GC")
            }
            if (testRunSettings.get<GCType>().gc == GC.NOOP) {
                add("-D")
                add("NOOP_GC")
            }
        }
        val successExecutable = compileSwift(swiftFiles!!.toList(), swiftExtraOpts)
        val testExecutable = TestExecutable(
            successExecutable.resultingArtifact,
            successExecutable.loggedData,
            listOf(TestName("objCExportTest$suffix"))
        )
        runExecutableAndVerify(testCase, testExecutable)

        // check Info.plist for expected bundle identifier
        val plistFName = if (targets.testTarget.family == Family.OSX) "Resources/Info.plist" else "Info.plist"
        val infoPlist = buildDir.resolve("$frameworkName.framework/$plistFName")
        val infoPlistContents = infoPlist.readText()
        assertTrue(infoPlistContents.contains(Regex("<key>CFBundleIdentifier</key>\\s*<string>foo.bar</string>"))) {
            "${infoPlist.absolutePath} does not contain expected pattern with `foo.bar`:\n$infoPlistContents"
        }
    }

    private fun generateObjCFramework(
        name: String,
        testCompilerArgs: List<String> = emptyList(),
        givenDependencies: Set<TestModule.Given> = emptySet(),
        checks: TestRunChecks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout),
        moduleName: String = name.replaceFirstChar { it.uppercase() },
    ): TestCase {
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)

        val testCase = generateObjCFrameworkTestCase(
            TestKind.STANDALONE_NO_TR,
            extras,
            moduleName,
            listOf(testSuiteDir.resolve(name).resolve("$name.kt")),
            TestCompilerArgs(
                testCompilerArgs + listOf("-module-name", moduleName, "-Xbinary=bundleId=$name")
            ),
            givenDependencies,
            checks = checks,
        )
        val objCFrameworkCompilation = testCompilationFactory.testCaseToObjCFrameworkCompilation(testCase, testRunSettings)
        val success = objCFrameworkCompilation.result.assertSuccess()
        codesign(success.resultingArtifact.frameworkDir.absolutePath)

        return testCase
    }

    private fun compileAndRunSwift(
        testName: String,
        testCase: TestCase,
        swiftExtraOpts: List<String> = emptyList(),
        testDir: File = testSuiteDir.resolve(testName),
    ) {
        val success =
            compileSwift(listOf(testDir.resolve("$testName.swift")), swiftExtraOpts)
        val testExecutable = TestExecutable(
            success.resultingArtifact,
            success.loggedData,
            listOf(TestName(testName))
        )
        runExecutableAndVerify(testCase, testExecutable)
    }

    private fun compileSwift(
        testSources: List<File>,
        swiftExtraOpts: List<String>,
    ): TestCompilationResult.Success<out TestCompilationArtifact.Executable> {
        // create a test provider and get main entry point
        val provider = createTestProvider(buildDir, testSources)
        val frameworkOpts = listOf(
            "-Xlinker", "-rpath", "-Xlinker", "@executable_path/Frameworks",
            "-Xlinker", "-rpath", "-Xlinker", buildDir.absolutePath,
            "-F", buildDir.absolutePath
        )
        return SwiftCompilation(
            testRunSettings,
            testSources + listOf(
                provider,
                testSuiteDir.resolve("main.swift")
            ),
            TestCompilationArtifact.Executable(buildDir.resolve("swiftTestExecutable")),
            swiftExtraOpts + frameworkOpts,
            outputFile = { executable -> executable.executableFile }
        ).result.assertSuccess()
    }
}
