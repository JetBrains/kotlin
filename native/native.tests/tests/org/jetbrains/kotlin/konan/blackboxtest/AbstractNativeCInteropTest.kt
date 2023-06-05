/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.blackboxtest.support.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationArtifact.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.*
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.*
import org.jetbrains.kotlin.konan.blackboxtest.support.util.*
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.CInteropHints
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Tag
import java.io.File

abstract class AbstractNativeCInteropFModulesTest : AbstractNativeCInteropTest() {
    override val fmodules = true

    override val defFileName: String = "pod1.def"
}

abstract class AbstractNativeCInteropNoFModulesTest : AbstractNativeCInteropTest() {
    override val fmodules = false

    override val defFileName: String = "pod1.def"
}

abstract class AbstractNativeCInteropIncludeCategoriesTest : AbstractNativeCInteropTest() {
    override val fmodules: Boolean
        get() = false

    override val defFileName: String
        get() = "dependency.def"
}

@Tag("cinterop")
abstract class AbstractNativeCInteropTest : AbstractNativeCInteropBaseTest() {
    abstract val fmodules: Boolean

    abstract val defFileName: String

    @Synchronized
    protected fun runTest(@TestDataFile testPath: String) {
        // FIXME: check the following failures under Android with -fmodules
        // fatal error: could not build module 'std'
        Assumptions.assumeFalse(
            this is AbstractNativeCInteropFModulesTest &&
                    targets.testTarget.family == Family.ANDROID
        )
        val testPathFull = getAbsoluteFile(testPath)
        val testDataDir = testPathFull.parentFile.parentFile
        val includeFolder = testDataDir.resolve("include")
        val defFile = testPathFull.resolve(defFileName)
        val defContents = defFile.readText().split("\n").map { it.trim() }
        val defHasObjC = defContents.any { it.endsWith("Objective-C") }
        Assumptions.assumeFalse(defHasObjC && !targets.testTarget.family.isAppleFamily)

        val defHasHeaders = defContents.any { it.startsWith("headers") }
        Assumptions.assumeFalse(fmodules && defHasHeaders)

        val goldenFile = if (testDataDir.name == "builtins")
            getBuiltinsGoldenFile(testPathFull)
        else
            getGoldenFile(testPathFull)
        val fmodulesArgs = if (fmodules) TestCompilerArgs("-compiler-option", "-fmodules") else TestCompilerArgs.EMPTY
        val includeArgs = if (testDataDir.name.startsWith("framework"))
            TestCompilerArgs("-compiler-option", "-F${testDataDir.canonicalPath}")
        else
            TestCompilerArgs("-compiler-option", "-I${includeFolder.canonicalPath}")

        val testCompilationResult = cinteropToLibrary(targets, defFile, buildDir, includeArgs + fmodulesArgs)
        // If we are running fmodules-specific test without -fmodules then we want to be sure that cinterop fails the way we want it to.
        if (!fmodules && testPath.endsWith("FModules/")) {
            val loggedData = (testCompilationResult as TestCompilationResult.CompilationToolFailure).loggedData
            val prettyMessage = CInteropHints.fmodulesHint
            assertTrue(loggedData.toString().contains(prettyMessage)) {
                "Test failed. CInterop compilation result was: $testCompilationResult"
            }
        } else {
            val klibContents = testCompilationResult.assertSuccess().resultingArtifact.getContents(kotlinNativeClassLoader.classLoader)
            val expectedContents = goldenFile.readText()
            assertEquals(StringUtilRt.convertLineSeparators(expectedContents), StringUtilRt.convertLineSeparators(klibContents)) {
                "Test failed. CInterop compilation result was: $testCompilationResult"
            }
        }
    }

    private fun getGoldenFile(testPathFull: File): File {
        return testPathFull.resolve("contents.gold.txt")
    }

    private fun getBuiltinsGoldenFile(testPathFull: File): File {
        val goldenFilePart = when (targets.testTarget) {
            KonanTarget.ANDROID_ARM32 -> "ARM32"
            KonanTarget.ANDROID_ARM64 -> "ARM64"
            KonanTarget.ANDROID_X64 -> "X64"
            KonanTarget.ANDROID_X86 -> "CPointerByteVar"
            KonanTarget.IOS_ARM32 -> "COpaquePointer"
            KonanTarget.IOS_ARM64 -> "CPointerByteVar"
            KonanTarget.IOS_SIMULATOR_ARM64 -> "CPointerByteVar"
            KonanTarget.IOS_X64 -> "X64"
            KonanTarget.LINUX_ARM32_HFP -> "ARM32"
            KonanTarget.LINUX_ARM64 -> "ARM64"
            KonanTarget.LINUX_MIPS32 -> "COpaquePointer"
            KonanTarget.LINUX_MIPSEL32 -> "COpaquePointer"
            KonanTarget.LINUX_X64 -> "X64"
            KonanTarget.MACOS_ARM64 -> "CPointerByteVar"
            KonanTarget.MACOS_X64 -> "X64"
            KonanTarget.MINGW_X64 -> "CPointerByteVar"
            KonanTarget.MINGW_X86 -> "CPointerByteVar"
            KonanTarget.TVOS_ARM64 -> "CPointerByteVar"
            KonanTarget.TVOS_SIMULATOR_ARM64 -> "CPointerByteVar"
            KonanTarget.TVOS_X64 -> "X64"
            KonanTarget.WASM32 -> "COpaquePointer"
            KonanTarget.WATCHOS_ARM32 -> "CPointerByteVar"
            KonanTarget.WATCHOS_ARM64 -> "CPointerByteVar"
            KonanTarget.WATCHOS_DEVICE_ARM64 -> "CPointerByteVar"
            KonanTarget.WATCHOS_SIMULATOR_ARM64 -> "CPointerByteVar"
            KonanTarget.WATCHOS_X64 -> "X64"
            KonanTarget.WATCHOS_X86 -> "CPointerByteVar"
            is KonanTarget.ZEPHYR -> "COpaquePointer"
        }
        return testPathFull.resolve("contents.gold.${goldenFilePart}.txt")
    }
}
