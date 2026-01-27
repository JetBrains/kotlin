/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.util.dumpMetadata
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEqualsToFile
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertIs

class ModularCinteropTest : AbstractNativeCInteropBaseTest() {

    @BeforeEach
    fun onlyOnAppleTargets() {
        // We are primarily interested in running these tests on Apple targets because we care about Objective-C, but in theory we could
        // support -fmodules tests on other hosts and targets as well even where we don't support Objective-C
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
    }
    @Test
    fun `cinterop modular import with -fmodule-map-file - sees modules`() {
        val testPathFull = getAbsoluteFile("native/native.tests/testData/CInterop/explicitModuleMapFile")
        val modulemap = testPathFull.resolve("foo.modulemap")
        val defFile = testPathFull.resolve("foo.def")
        val goldenFile = testPathFull.resolve("output.txt")

        val modulemapArgs = TestCInteropArgs("-compiler-option", "-fmodule-map-file=${modulemap}", "-compiler-option", "-fmodules")
        val klib = cinteropToLibrary(defFile, buildDir, modulemapArgs).assertSuccess().resultingArtifact
        val metadata = klib.dumpMetadata(kotlinNativeClassLoader.classLoader, false, null)
        assertEqualsToFile(goldenFile, metadata)
    }

    @Test
    fun `skipNonImportableModules - is disabled by default - which leads to cinterop failure when some modules don't import`() {
        val testPathFull = getAbsoluteFile("native/native.tests/testData/CInterop/skipNonImportableModules/someModulesFailToImport")
        val defFile = testPathFull.resolve("default_behavior.def")

        val modulemapArgs = TestCInteropArgs("-compiler-option", "-I${testPathFull}")
        val result = cinteropToLibrary(defFile, buildDir, modulemapArgs)
        assertIs<TestCompilationResult.CompilationToolFailure>(result)
        val actualFailure = result.loggedData.toString()
        // FIXME: We actually want to see the "'iostream' file not found", but it doesn't display right now KT-84023
        assertContains(actualFailure, "fatal error: could not build module 'failure'")
    }

    @Test
    fun `skipNonImportableModules - produces cinterop klib - when only some modules fail to import`() {
        val testPathFull = getAbsoluteFile("native/native.tests/testData/CInterop/skipNonImportableModules/someModulesFailToImport")
        val defFile = testPathFull.resolve("skip_non_importable_modules.def")
        val goldenFile = testPathFull.resolve("output.txt")

        val modulemapArgs = TestCInteropArgs("-compiler-option", "-I${testPathFull}", "-compiler-option", "-fmodules")
        val result = cinteropToLibrary(defFile, buildDir, modulemapArgs).assertSuccess()
        val klib = result.resultingArtifact
        val metadata = klib.dumpMetadata(kotlinNativeClassLoader.classLoader, false, null)
        assertEqualsToFile(goldenFile, metadata)
    }

    @Test
    fun `skipNonImportableModules - emits failure - when all modules fail to import`() {
        val testPathFull = getAbsoluteFile("native/native.tests/testData/CInterop/skipNonImportableModules/allModulesFailToImport")
        val defFile = testPathFull.resolve("foo.def")

        val modulemapArgs = TestCInteropArgs("-compiler-option", "-I${testPathFull}", "-compiler-option", "-fmodules")
        val result = cinteropToLibrary(defFile, buildDir, modulemapArgs)
        assertIs<TestCompilationResult.CompilationToolFailure>(result)
        val actualFailure = result.loggedData.toString()
        assertContains(actualFailure, "error: \"non-importable module failure_one\"")
        assertContains(actualFailure, "error: \"non-importable module failure_two\"")
    }

    @Test
    fun `skipNonImportableModules - no modules imported`() {
        val testPathFull = getAbsoluteFile("native/native.tests/testData/CInterop/skipNonImportableModules/noModulesImported")
        val defFile = testPathFull.resolve("foo.def")
        val goldenFile = testPathFull.resolve("output.txt")

        val modulemapArgs = TestCInteropArgs("-compiler-option", "-I${testPathFull}", "-compiler-option", "-fmodules")
        val result = cinteropToLibrary(defFile, buildDir, modulemapArgs).assertSuccess()
        val klib = result.resultingArtifact
        val metadata = klib.dumpMetadata(kotlinNativeClassLoader.classLoader, false, null)
        assertEqualsToFile(goldenFile, metadata)
    }
}
