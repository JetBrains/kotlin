/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.konan.library.components.KlibBitcodeConstants.KLIB_ARM64E_FOLDER_NAME
import org.jetbrains.kotlin.konan.library.components.KlibBitcodeConstants.KLIB_PROPERTY_NATIVE_BITCODE_VARIANTS
import org.jetbrains.kotlin.konan.target.AppleConfigurables
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.ClassLevelProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.TestKind
import org.jetbrains.kotlin.konan.test.blackbox.support.TestModule
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.ExistingDependency
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.ObjCFrameworkCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationDependencyType
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationFactory
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.BinaryLibraryKind
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeHome
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.configurables
import org.jetbrains.kotlin.konan.test.blackbox.support.util.ClangDistribution
import org.jetbrains.kotlin.konan.test.blackbox.support.util.compileWithClang
import org.jetbrains.kotlin.library.KLIB_PROPERTY_COMPILER_VERSION
import org.jetbrains.kotlin.library.KLIB_PROPERTY_NATIVE_TARGETS
import org.jetbrains.kotlin.native.executors.runProcess
import org.jetbrains.kotlin.test.KtAssert.fail
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Properties
import kotlin.time.Duration

/**
 * Tests `-Xbinary=arm64e=true`. arm64e binaries cannot run on simulators or macOS hosts,
 * so these tests check the generated LLVM IR. They don't use caches, which would move code out of it, unless they test caches.
 */
@TestDataPath("\$PROJECT_ROOT")
@EnforcedProperty(ClassLevelProperty.CACHE_MODE, "NO")
class Arm64eTest : AbstractNativeSimpleTest() {
    private val testSuiteDir = ForTestCompileRuntime.transformTestDataPath("native/native.tests/testData/arm64e")
    private val testCompilationFactory = TestCompilationFactory()

    @BeforeEach
    fun assumeIosArm64() {
        Assumptions.assumeTrue(targets.testTarget == KonanTarget.IOS_ARM64)
    }

    @Test
    fun testTarget() = compileFrameworkAndCheckIr(testSuiteDir.resolve("target.kt"), "LinkBitcodeDependencies")

    @Test
    fun testPtrAuthAbiVersion() = compileFrameworkAndCheckIr(testSuiteDir.resolve("ptrauth_abi_version.kt"), "Codegen")

    @Test
    fun testStaticCaches() {
        val library = staticCachesLibrary()
        val cacheRoot = buildDir.resolve("caches")
        compileFramework(
            testSuiteDir.resolve("static_caches/static_caches.kt"),
            listOf("-Xbinary=arm64e=true") + autoCacheArgs(library, cacheRoot),
            givenDependencies = setOf(TestModule.Given(library.klibFile)),
        ).result.assertSuccess()

        // arm64e caches have a flavor of their own.
        val cacheDir = cacheRoot.listFiles().orEmpty().single { it.isDirectory }
        assertTrue("-arm64e" in cacheDir.name) { "Expected an arm64e cache flavor, got ${cacheDir.name}" }
        val archives = cacheDir.walkTopDown().filter { it.extension == "a" }.toList()
        assertTrue(archives.isNotEmpty()) { "No cache in ${cacheDir.absolutePath}" }
        archives.forEach(::assertArm64eObjects)
    }

    @Test
    fun testStaticLibrary() {
        val library = staticCachesLibrary()
        val staticLibrary = testCompilationFactory.testCaseToBinaryLibrary(
            generateObjCFrameworkTestCase(
                TestKind.STANDALONE_NO_TR,
                TestCase.NoTestRunnerExtras("There's no entrypoint in a static library"),
                "static_library",
                listOf(testSuiteDir.resolve("static_caches/static_caches.kt")),
                TestCompilerArgs(listOf("-Xbinary=arm64e=true") + autoCacheArgs(library, buildDir.resolve("caches"))),
                setOf(TestModule.Given(library.klibFile)),
            ),
            testRunSettings,
            BinaryLibraryKind.STATIC,
        ).result.assertSuccess().resultingArtifact.libraryFile.let {
            // The compiler adds the prefix of static libraries to the name.
            it.resolveSibling(targets.testTarget.family.staticPrefix + it.name)
        }

        // The static library contains the caches, which libtool leaves without objects if they have no pointer authentication
        // ABI version.
        assertArm64eObjects(staticLibrary)
        val symbols = runProcess(llvmTool("llvm-nm"), "--defined-only", "--just-symbol-name", staticLibrary.absolutePath).stdout.lines()
        assertTrue("_kfun:library#value(library.Base){}kotlin.Int" in symbols) { "No code of the cached library in ${staticLibrary.absolutePath}" }
    }

    @Test
    fun testFunctionAttributes() = compileFrameworkAndCheckIr(testSuiteDir.resolve("function_attributes.kt"), "Codegen")

    @Test
    fun testFunctionAttributesWithoutArm64e() =
        compileFrameworkAndCheckIr(testSuiteDir.resolve("function_attributes_without_arm64e.kt"), "Codegen", arm64e = false)

    @Test
    fun testCFunctionPointers() = compileFrameworkAndCheckIr(
        testSuiteDir.resolve("c_function_pointers.kt"), "Codegen", listOf("-Xbinary=sourceInfoType=libbacktrace")
    )

    @Test
    fun testObjCMessenger() =
        compileFrameworkAndCheckIr(testSuiteDir.resolve("objc/messenger.kt"), "Codegen", givenDependencies = setOf(objcLibrary()))

    @Test
    fun testVirtualCalls() = compileFrameworkAndCheckIr(testSuiteDir.resolve("virtual_calls.kt"), "Codegen")

    @Test
    fun testObjCClassData() = compileFrameworkAndCheckIr(testSuiteDir.resolve("objc_class_data.kt"), "Codegen")

    @Test
    fun testKotlinSubclassOfObjCClass() =
        compileFrameworkAndCheckIr(testSuiteDir.resolve("objc/subclass.kt"), "Codegen", givenDependencies = setOf(objcLibrary()))

    @Test
    fun testBlocks() = compileFrameworkAndCheckIr(testSuiteDir.resolve("blocks.kt"), "Codegen")

    @Test
    fun testMissingArm64eBitcode() {
        val result = compileFramework(
            testSuiteDir.resolve("c_stubs/c_stubs.kt"),
            listOf("-Xbinary=arm64e=true"),
            givenDependencies = setOf(TestModule.Given(cinterop("c_stubs").klibFile)),
        ).result
        assertTrue(result is TestCompilationResult.CompilationToolFailure && "of library 'c_stubs.def' is compiled for 'arm64-apple-ios" in result.loggedData.toolOutput) {
            "Expected the arm64e framework to reject arm64 bitcode, got: $result"
        }
    }

    @Test
    fun testNativeLibraryCompiledForArm64e() {
        val source = testSuiteDir.resolve("native_library/native_library.kt")
        // Like the library that a one-stage compilation makes, this library has the -native-library bitcode as its ordinary bitcode.
        val library = compileToLibrary(
            source, buildDir.resolve("library"),
            TestCompilerArgs("-native-library", compileNativeLibrary(arm64e = true).absolutePath), emptyList(),
        )
        linkFramework(
            library, listOf("-Xsave-llvm-ir-after=LinkBitcodeDependencies", "-Xsave-llvm-ir-directory=${buildDir.absolutePath}")
        ).assertSuccess()
        checkIr(source, "LinkBitcodeDependencies")
    }

    @Test
    fun testNativeLibraryCompiledForArm64() {
        val library = compileToLibrary(testSuiteDir.resolve("native_library/native_library.kt"), buildDir.resolve("library"))
        val result = linkFramework(library, listOf("-native-library", compileNativeLibrary(arm64e = false).absolutePath))
        assertTrue(result is TestCompilationResult.CompilationToolFailure && "is compiled for 'arm64-apple-ios" in result.loggedData.toolOutput) {
            "Expected the arm64e framework to reject arm64 bitcode, got: $result"
        }
    }

    @Test
    fun testCInteropArm64eBitcode() {
        val testName = "arm64e_bitcode"
        val interopLibrary = cinterop(
            testName,
            listOf(
                "-nopack", "-Xarm64e-bitcode",
                "-Xccall-mode", "indirect", "-Xcompile-source", testSuiteDir.resolve("$testName/compiled.cpp").absolutePath,
            ),
        ).klibFile
        val targetDir = interopLibrary.resolve("default/targets/ios_arm64")
        val bitcodeFiles = setOf("cstubs.bc", "0_compiled.bc")
        assertEquals(bitcodeFiles, targetDir.resolve("native").list()?.toSet())
        assertEquals(bitcodeFiles, targetDir.resolve("$KLIB_ARM64E_FOLDER_NAME/native").list()?.toSet())
        val manifest = interopLibrary.resolve("default/manifest").bufferedReader().use { Properties().apply { load(it) } }
        // Compilers and tools that don't know the arm64e variant see an ordinary ios_arm64 library.
        assertEquals("ios_arm64", manifest.getProperty(KLIB_PROPERTY_NATIVE_TARGETS))
        assertEquals(KLIB_ARM64E_FOLDER_NAME, manifest.getProperty(KLIB_PROPERTY_NATIVE_BITCODE_VARIANTS))

        compileFrameworkAndCheckIr(
            testSuiteDir.resolve("$testName/$testName.kt"), "LinkBitcodeDependencies",
            givenDependencies = setOf(TestModule.Given(interopLibrary)),
        )
    }

    @Test
    fun testPlatformLibraries() {
        val nativeHome = testRunSettings.get<KotlinNativeHome>()
        val manifest = nativeHome.librariesDir
            .resolve("platform/ios_arm64/org.jetbrains.kotlin.native.platform.Foundation/default/manifest")
            .bufferedReader().use { Properties().apply { load(it) } }
        // Platform libraries built by the bootstrap compiler have no arm64e bitcode.
        Assumptions.assumeTrue(manifest.getProperty(KLIB_PROPERTY_COMPILER_VERSION) == nativeHome.properties.getProperty("compilerVersion")) {
            "The platform libraries were built by another compiler"
        }
        assertEquals(KLIB_ARM64E_FOLDER_NAME, manifest.getProperty(KLIB_PROPERTY_NATIVE_BITCODE_VARIANTS))
        compileFrameworkAndCheckIr(testSuiteDir.resolve("platform_libraries.kt"), "LinkBitcodeDependencies")
    }

    /** An Objective-C library without bitcode: with direct C calls, cinterop compiles no stubs. */
    private fun objcLibrary() = TestModule.Given(cinterop("objc", listOf("-Xccall-mode", "direct")).klibFile)

    private fun cinterop(testName: String, cinteropArgs: List<String> = emptyList()): TestCompilationArtifact.KLIB =
        cinteropToLibrary(
            defFile = testSuiteDir.resolve(testName).resolve("$testName.def"),
            outputDir = buildDir,
            freeCompilerArgs = TestCompilerArgs(emptyList(), cinteropArgs = cinteropArgs),
        ).assertSuccess().resultingArtifact

    private fun staticCachesLibrary() = compileToLibrary(testSuiteDir.resolve("static_caches/library"), buildDir.resolve("library"))

    /** Arguments that build static caches of [library] in [cacheRoot]. */
    private fun autoCacheArgs(library: TestCompilationArtifact.KLIB, cacheRoot: File): List<String> {
        cacheRoot.mkdirs()
        return listOf("-Xauto-cache-from=${library.klibFile.parentFile.absolutePath}", "-Xauto-cache-dir=${cacheRoot.absolutePath}")
    }

    /** Asserts that [archive] contains only arm64e objects with the pointer authentication ABI. */
    private fun assertArm64eObjects(archive: File) {
        val headers = runProcess(llvmTool("llvm-objdump"), "--macho", "--private-header", archive.absolutePath)
            .stdout.lines().filter { it.startsWith("MH_MAGIC") }
        assertTrue(headers.isNotEmpty() && headers.all { it.matches(Regex("""MH_MAGIC_64\s+ARM64\s+E\s+PAC00\s.*""")) }) {
            "Expected arm64e objects with the pointer authentication ABI in ${archive.absolutePath}, got: $headers"
        }
    }

    private fun compileNativeLibrary(arm64e: Boolean): File {
        val configurables = testRunSettings.configurables as AppleConfigurables
        return compileWithClang(
            testRunSettings,
            clangDistribution = ClangDistribution.Llvm, // To be compatible with the K/N LLVM IR.
            sourceFiles = listOf(testSuiteDir.resolve("native_library/native_library.c")),
            outputFile = buildDir.resolve(if (arm64e) "native_library.bc" else "native_library_arm64.bc"),
            additionalClangFlags = listOf("-emit-llvm", "-c") +
                    if (arm64e) listOf("-target", "arm64e-apple-ios${configurables.osVersionMin}") else emptyList(),
            debug = false,
        ).assertSuccess().resultingArtifact.executableFile
    }

    /** Links an arm64e framework from [library] alone, as the second stage of a compilation does. */
    private fun linkFramework(library: TestCompilationArtifact.KLIB, compilerArgs: List<String>) = ObjCFrameworkCompilation(
        testRunSettings,
        TestCompilerArgs(listOf("-Xbinary=arm64e=true", "-Xbinary=bundleId=native_library") + compilerArgs),
        sourceModules = emptySet(),
        dependencies = listOf(ExistingDependency(library, TestCompilationDependencyType.IncludedLibrary)),
        expectedArtifact = TestCompilationArtifact.ObjCFramework(buildDir, "native_library"),
    ).result

    private fun llvmTool(name: String) = "${testRunSettings.configurables.absoluteLlvmHome}/bin/$name"

    private fun compileFramework(
        source: File,
        compilerArgs: List<String>,
        givenDependencies: Set<TestModule.Given> = emptySet(),
        checks: TestRunChecks = TestRunChecks.Default(Duration.ZERO),
    ) = testCompilationFactory.testCaseToObjCFrameworkCompilation(
        generateObjCFrameworkTestCase(
            TestKind.STANDALONE_NO_TR,
            TestCase.NoTestRunnerExtras("There's no entrypoint in a framework"),
            source.nameWithoutExtension,
            listOf(source),
            TestCompilerArgs(compilerArgs + "-Xbinary=bundleId=${source.nameWithoutExtension}"),
            givenDependencies,
            checks,
        ),
        testRunSettings,
    )

    private fun compileFrameworkAndCheckIr(
        testDataFile: File,
        stage: String,
        compilerArgs: List<String> = emptyList(),
        arm64e: Boolean = true,
        givenDependencies: Set<TestModule.Given> = emptySet(),
    ) {
        val fileCheckMatcher = TestRunCheck.FileCheckMatcher(testRunSettings, testDataFile)
        compileFramework(
            testDataFile,
            listOfNotNull("-Xbinary=arm64e=true".takeIf { arm64e }) + compilerArgs + listOf(
                "-Xsave-llvm-ir-after=$stage",
                "-Xsave-llvm-ir-directory=${buildDir.absolutePath}",
            ),
            givenDependencies,
            // KT-64879: FileCheck is a run check, so it is called directly here.
            TestRunChecks.Default(Duration.ZERO).copy(fileCheckMatcher = fileCheckMatcher),
        ).result.assertSuccess()
        checkIr(testDataFile, stage, fileCheckMatcher)
    }

    private fun checkIr(
        testDataFile: File,
        stage: String,
        fileCheckMatcher: TestRunCheck.FileCheckMatcher = TestRunCheck.FileCheckMatcher(testRunSettings, testDataFile),
    ) {
        val irFile = buildDir.resolve("out.$stage.ll")
        val result = fileCheckMatcher.doFileCheck(irFile)
        if (result.stdout.isNotEmpty() || result.stderr.isNotEmpty()) {
            fail(
                "FileCheck of ${irFile.absolutePath} with '--check-prefixes ${fileCheckMatcher.prefixes}' failed:\n" +
                        "${result.stdout}\n${result.stderr}"
            )
        }
    }
}
