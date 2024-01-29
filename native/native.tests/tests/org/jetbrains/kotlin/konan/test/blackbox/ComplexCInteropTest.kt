/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.konan.target.isSimulator
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.group.FirPipeline
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.*
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.util.ClangDistribution
import org.jetbrains.kotlin.konan.test.blackbox.support.util.compileWithClang
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

@TestDataPath("\$PROJECT_ROOT")
class ClassicComplexCInteropTest : ComplexCInteropTestBase()

@FirPipeline
@Tag("frontend-fir")
@TestDataPath("\$PROJECT_ROOT")
class FirComplexCInteropTest : ComplexCInteropTestBase()

abstract class ComplexCInteropTestBase : AbstractNativeSimpleTest() {
    private val interopObjCDir = File("native/native.tests/testData/interop/objc")

    @Test
    @TestMetadata("smoke.kt")
    fun testInteropObjCSmokeGC() {
        Assumptions.assumeTrue(
            testRunSettings.get<GCType>() != GCType.NOOP
                    || testRunSettings.get<CacheMode>() != CacheMode.WithoutCache // TODO: Remove line after fix of KT-63944
        )
        testInteropObjCSmoke("smoke")
    }

    @Test
    @TestMetadata("smoke_noopgc.kt")
    fun testInteropObjCSmokeNoopGC() {
        Assumptions.assumeTrue(testRunSettings.get<GCType>() == GCType.NOOP)
        Assumptions.assumeTrue(testRunSettings.get<CacheMode>() == CacheMode.WithoutCache) // TODO: Remove line after fix of KT-63944
        testInteropObjCSmoke("smoke_noopgc")
    }

    private fun testInteropObjCSmoke(ktFilePrefix: String) {
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        val mSources = listOf(interopObjCDir.resolve("smoke.m"))
        val dylib = compileDylib("objcsmoke", mSources)
        println(dylib)
        if (testRunSettings.configurables.targetTriple.isSimulator)
            codesign(dylib.resultingArtifact.path)
        val stringsdict = interopObjCDir.resolve("Localizable.stringsdict")
        stringsdict.copyTo(buildDir.resolve("en.lproj/Localizable.stringsdict"), overwrite = true)

        val cinteropKlib = cinteropToLibrary(
            targets = targets,
            defFile = interopObjCDir.resolve("objcSmoke.def"),
            outputDir = buildDir,
            freeCompilerArgs = TestCompilerArgs(emptyList(), cinteropArgs = listOf("-header", "smoke.h"))
        ).assertSuccess().resultingArtifact
        println(cinteropKlib)

        val testCase = generateTestCaseWithSingleFile(
            sourceFile = interopObjCDir.resolve("$ktFilePrefix.kt"),
            moduleName = "smoke",
            freeCompilerArgs = TestCompilerArgs(
                "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                "-linker-option",
                "-L${buildDir.absolutePath}"
            ),
            testKind = TestKind.STANDALONE_NO_TR,
            extras = TestCase.NoTestRunnerExtras("main"),
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout).copy(
                outputDataFile = TestRunCheck.OutputDataFile(file = interopObjCDir.resolve("$ktFilePrefix.out"))
            )
        )
        val success = compileToExecutable(testCase, cinteropKlib.asLibraryDependency()).assertSuccess()
        println(success)
        val testExecutable = TestExecutable(
            success.resultingArtifact,
            success.loggedData,
            listOf(TestName("smoke"))
        )
        runExecutableAndVerify(testCase, testExecutable)
    }

    @Test
    @TestMetadata("tests")
    fun testInteropObjCTests() {
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        val mSources = interopObjCDir.resolve("tests").listFiles { file: File -> file.name.endsWith(".m") }!!.toList()
        val dylib = compileDylib("objctests", mSources)
        if (testRunSettings.configurables.targetTriple.isSimulator)
            codesign(dylib.resultingArtifact.path)
        println(dylib)

        val hFiles = interopObjCDir.resolve("tests").listFiles { file: File -> file.name.endsWith(".h") }!!.toList()
        val cinteropKlib = cinteropToLibrary(
            targets = targets,
            defFile = interopObjCDir.resolve("objcTests.def"),
            outputDir = buildDir,
            freeCompilerArgs = TestCompilerArgs(emptyList(), cinteropArgs = hFiles.flatMap { listOf("-header", "tests/${it.name}") })
        ).assertSuccess().resultingArtifact
        println(cinteropKlib)

        val ignoredTestGTestPatterns = if (testRunSettings.get<GCType>() != GCType.NOOP) emptySet() else setOf(
            "Kt41811Kt.*",
            "CustomStringKt.testCustomString",
            "Kt42482Kt.testKT42482",
            "ObjcWeakRefsKt.testObjCWeakRef",
            "WeakRefsKt.testWeakRefs",
        )
        val testCase = generateTestCaseWithSingleModule(
            moduleDir = interopObjCDir.resolve("tests"),
            freeCompilerArgs = TestCompilerArgs(
                "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                "-opt-in=kotlin.native.internal.InternalForKotlinNative",
                "-XXLanguage:+ImplicitSignedToUnsignedIntegerConversion",
                "-tr", "-e", "main", "-linker-option", "-L${buildDir.absolutePath}"
            ),
            extras = TestCase.WithTestRunnerExtras(TestRunnerType.DEFAULT, ignoredTestGTestPatterns),
        )
        val success = compileToExecutable(testCase, cinteropKlib.asLibraryDependency()).assertSuccess()
        println(success)
        val testExecutable = TestExecutable(
            success.resultingArtifact,
            success.loggedData,
            listOf(TestName("interop_objc_tests"))
        )
        runExecutableAndVerify(testCase, testExecutable)
    }

    private fun compileDylib(name: String, mSources: List<File>): TestCompilationResult.Success<out TestCompilationArtifact.Executable> {
        val clangResult = compileWithClang(
            clangDistribution = ClangDistribution.Llvm,
            sourceFiles = mSources,
            includeDirectories = emptyList(),
            outputFile = buildDir.resolve("lib$name.dylib"),
            libraryDirectories = emptyList(),
            libraries = emptyList(),
            additionalClangFlags = listOf(
                "-DNS_FORMAT_ARGUMENT(A)=", "-lobjc", "-fobjc-arc", "-fPIC", "-shared",
                // Enable ARC optimizations to prevent some objects from leaking in Obj-C code due to exceptions:
                "-O2"
            ),
            fmodules = false, // with `-fmodules`, ld cannot find symbol `_assert`
        ).assertSuccess()
        return clangResult
    }
}