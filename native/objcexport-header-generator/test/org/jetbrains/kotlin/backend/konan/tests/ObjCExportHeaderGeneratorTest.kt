/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.tests

import org.jetbrains.kotlin.backend.konan.testUtils.headersTestDataDir
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.fail

/**
 * ## Test Scope
 * This test will cover the generation of 'ObjC' headers.
 *
 * The input of the test are Kotlin source files;
 * The output is the generated header files;
 * The output will be compared to already checked in golden files.
 *
 * ## How to create a new test
 * Every test has a corresponding 'root' directory.
 * All directories are found in `native/objcexport-header-generator/testData/headers`.
 *
 * 1) Create new root directory (e.g. myTest) in testData/headers
 * 2) Place kotlin files into the directory e.g. testData/headers/myTest/Foo.kt
 * 3) Create a test function and call ` doTest(headersTestDataDir.resolve("myTest"))`
 * 4) The first invocation will fail the test, but generates the header that can be checked in (if sufficient)
 */
class ObjCExportHeaderGeneratorTest(val generator: HeaderGenerator) {

    @Test
    fun `test - simpleClass`() {
        doTest(headersTestDataDir.resolve("simpleClass"))
    }

    @Test
    fun `test - simpleInterface`() {
        doTest(headersTestDataDir.resolve("simpleInterface"))
    }

    @Test
    fun `test - simpleEnumClass`() {
        doTest(headersTestDataDir.resolve("simpleEnumClass"))
    }

    @Test
    fun `test - simpleObject`() {
        doTest(headersTestDataDir.resolve("simpleObject"))
    }

    @Test
    fun `test - topLevelFunction`() {
        doTest(headersTestDataDir.resolve("topLevelFunction"))
    }

    @Test
    fun `test - topLevelProperty`() {
        doTest(headersTestDataDir.resolve("topLevelProperty"))
    }

    @Test
    fun `test - sameClassNameInDifferentPackage`() {
        doTest(headersTestDataDir.resolve("sameClassNameInDifferentPackage"))
    }

    @Test
    fun `test - samePropertyAndFunctionName`() {
        doTest(headersTestDataDir.resolve("samePropertyAndFunctionName"))
    }

    @Test
    fun `test - classImplementingInterface`() {
        doTest(headersTestDataDir.resolve("classImplementingInterface"))
    }

    @Test
    fun `test - interfaceImplementingInterface`() {
        doTest(headersTestDataDir.resolve("interfaceImplementingInterface"))
    }

    @Test
    fun `test - classWithObjCNameAnnotation`() {
        doTest(headersTestDataDir.resolve("classWithObjCNameAnnotation"))
    }

    @Test
    fun `test - functionWithObjCNameAnnotation`() {
        doTest(headersTestDataDir.resolve("functionWithObjCNameAnnotation"))
    }

    @Test
    fun `test - classWithKDoc`() {
        doTest(headersTestDataDir.resolve("classWithKDoc"))
    }

    @Test
    fun `test  - classWithHidesFromObjCAnnotation`() {
        doTest(headersTestDataDir.resolve("classWithHidesFromObjCAnnotation"))
    }

    @Test
    fun `test - functionWithThrowsAnnotation`() {
        doTest(headersTestDataDir.resolve("functionWithThrowsAnnotation"))
    }

    @Test
    fun `test - functionWithErrorType`() {
        doTest(headersTestDataDir.resolve("functionWithErrorType"))
    }

    @Test
    fun `test - kdocWithBlockTags`() {
        doTest(headersTestDataDir.resolve("kdocWithBlockTags"))
    }

    fun interface HeaderGenerator {
        fun generateHeaders(root: File): String
    }

    private fun doTest(root: File) {
        if (!root.isDirectory) fail("Expected ${root.absolutePath} to be directory")
        val generatedHeaders = generator.generateHeaders(root)
        KotlinTestUtils.assertEqualsToFile(root.resolve("!${root.nameWithoutExtension}.h"), generatedHeaders)
    }
}
