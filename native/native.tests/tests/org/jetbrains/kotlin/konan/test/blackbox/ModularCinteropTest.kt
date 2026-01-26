/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.util.dumpMetadata
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEqualsToFile
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test

class ModularCinteropTest : AbstractNativeCInteropBaseTest() {

    @Test
    fun test() {
        Assumptions.assumeTrue(targets.hostTarget.family.isAppleFamily && targets.testTarget.family.isAppleFamily)

        val testPathFull = getAbsoluteFile(TEST_DATA_DIR)
        val modulemap = testPathFull.resolve("foo.modulemap")
        val defFile = testPathFull.resolve("foo.def")
        val goldenFile = testPathFull.resolve("output.txt")

        val modulemapArgs = TestCInteropArgs("-compiler-option", "-fmodule-map-file=${modulemap}", "-compiler-option", "-fmodules")
        val klib = cinteropToLibrary(defFile, buildDir, modulemapArgs).assertSuccess().resultingArtifact
        val metadata = klib.dumpMetadata(kotlinNativeClassLoader.classLoader, false, null)
        assertEqualsToFile(goldenFile, metadata)
    }

    companion object {
        private const val TEST_DATA_DIR = "native/native.tests/testData/CInterop/explicitModuleMapFile"
    }
}
