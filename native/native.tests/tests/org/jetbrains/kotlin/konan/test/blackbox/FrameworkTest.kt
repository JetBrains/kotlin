/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.group.ClassicPipeline
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.*
import org.jetbrains.kotlin.konan.test.blackbox.support.util.createTestProvider
import org.jetbrains.kotlin.native.executors.runProcess
import org.jetbrains.kotlin.test.KotlinTestUtils.assertEqualsToFile
import org.jetbrains.kotlin.test.KtAssert.fail
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.time.Duration

@ClassicPipeline()
@TestDataPath("\$PROJECT_ROOT")
class ClassicFrameworkTest : FrameworkTestBase()

@TestDataPath("\$PROJECT_ROOT")
class FirFrameworkTest : FrameworkTestBase()

abstract class FrameworkTestBase : AbstractNativeSimpleTest() {
    private val testSuiteDir = File("native/native.tests/testData/framework")
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
        val testCase = generateObjCFramework(testName)
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
    fun testKT42397() {
        val testName = "kt42397"
        val testCase = generateObjCFramework(testName)
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
    fun testForwardDeclarations() {
        val testName = "forwardDeclarations"
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        val interopLibrary = compileCInterop(testName)

        val testCase = generateObjCFramework(testName, emptyList(), setOf(TestModule.Given(interopLibrary.klibFile)))
        compileAndRunSwift(testName, testCase)
    }

    private fun compileCInterop(testName: String, freeCInteropArgs: TestCompilerArgs = TestCompilerArgs.EMPTY) =
        cinteropToLibrary(
            targets = targets,
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
        val swiftExtraOpts = if (testRunSettings.get<GCScheduler>() != GCScheduler.AGGRESSIVE) listOf() else
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
        Assumptions.assumeFalse(testRunSettings.get<GCType>() == GCType.NOOP) { "Test requires GC to actually happen" }

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
    fun objCExportTest() {
        objCExportTestImpl("", emptyList(), emptyList(), false, true)
    }

    @Test
    fun objCExportTestNoGenerics() {
        objCExportTestImpl("NoGenerics", listOf("-Xno-objc-generics"),
                           listOf("-D", "NO_GENERICS"), false, true)
    }

    @Test
    fun objCExportTestLegacySuspendUnit() {
        objCExportTestImpl("LegacySuspendUnit", listOf("-Xbinary=unitSuspendFunctionObjCExport=legacy"),
                           listOf("-D", "LEGACY_SUSPEND_UNIT_FUNCTION_EXPORT"), false, true)
    }

    @Test
    fun objCExportTestNoSwiftMemberNameMangling() {
        objCExportTestImpl("NoSwiftMemberNameMangling", listOf("-Xbinary=objcExportDisableSwiftMemberNameMangling=true"),
                           listOf("-D", "DISABLE_MEMBER_NAME_MANGLING"), false, false)
    }

    @Test
    fun objCExportTestNoInterfaceMemberNameMangling() {
        objCExportTestImpl("NoInterfaceMemberNameMangling", listOf("-Xbinary=objcExportIgnoreInterfaceMethodCollisions=true"),
                           listOf("-D", "DISABLE_INTERFACE_METHOD_NAME_MANGLING"), false, false)
    }

    @Test
    fun objCExportTestStatic() {
        objCExportTestImpl("Static", listOf("-Xbinary=objcExportSuspendFunctionLaunchThreadRestriction=main"),
                           listOf("-D", "DISALLOW_SUSPEND_ANY_THREAD"), true, false)
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
        needLazyHeaderCheck: Boolean,
    ) {
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        val doLazyHeaderCheck = needLazyHeaderCheck && testRunSettings.get<PipelineType>() == PipelineType.K1
        val lazyHeader: File = buildDir.resolve("lazy-$suffix.h").also { it.delete() } // Clean up lazy header after previous runs

        // Compile a couple of KLIBs
        val library = compileToLibrary(
            testSuiteDir.resolve("objcexport/library"),
            buildDir,
            TestCompilerArgs("-Xshort-module-name=MyLibrary", "-module-name", "org.jetbrains.kotlin.native.test-library"),
            emptyList(),
        )
        val noEnumEntries = compileToLibrary(
            testSuiteDir.resolve("objcexport/noEnumEntries"),
            buildDir,
            TestCompilerArgs(
                "-Xshort-module-name=NoEnumEntriesLibrary", "-XXLanguage:-EnumEntries",
                "-module-name", "org.jetbrains.kotlin.native.test-no-enum-entries-library",
            ),
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
                    "-Xemit-lazy-objc-header=${lazyHeader.absolutePath}".takeIf { doLazyHeaderCheck },
                )
            ),
            givenDependencies = setOf(TestModule.Given(library.klibFile), TestModule.Given(noEnumEntries.klibFile)),
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout * 5), // objcexport is a test suite on its own, increase the default timeout
        )
        testCompilationFactory.testCaseToObjCFrameworkCompilation(testCase, testRunSettings, listOf(noEnumEntries)).result.assertSuccess()

        // compile Swift sources using generated ObjC framework
        val swiftFiles = objcExportTestSuiteDir.listFiles { file: File -> file.name.endsWith(".swift") }
        assertTrue(swiftFiles != null && swiftFiles.isNotEmpty()) {
            "Some .swift files must be in test folder $objcExportTestSuiteDir"
        }
        val swiftExtraOpts = buildList {
            addAll(swiftOpts)
            if (testRunSettings.get<GCScheduler>() == GCScheduler.AGGRESSIVE) {
                add("-D")
                add("AGGRESSIVE_GC")
            }
            if (testRunSettings.get<GCType>() == GCType.NOOP) {
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

        if (doLazyHeaderCheck) {
            val expectedLazyHeaderName = "expectedLazy/expectedLazy${suffix}.h"
            val expectedLazyHeader = objcExportTestSuiteDir.resolve(expectedLazyHeaderName)
            if (!expectedLazyHeader.exists() || expectedLazyHeader.readLines() != lazyHeader.readLines()) {
                runProcess("diff", "-u", expectedLazyHeader.absolutePath, lazyHeader.absolutePath)
                lazyHeader.copyTo(expectedLazyHeader, overwrite = true)
                fail("$expectedLazyHeader file patched;\nPlease review this change and commit the patch, if change is correct")
            }
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
