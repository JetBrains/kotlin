/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.tests

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportHeaderGenerator
import org.jetbrains.kotlin.backend.konan.testUtils.AbstractFE10ObjCExportHeaderGeneratorTest
import org.junit.jupiter.api.Test

/**
 * ## Test Scope
 * This test will cover the generation of 'objc' headers.
 * The corresponding class in would be [ObjCExportHeaderGenerator].
 *
 * The input of the test are Kotlin source files;
 * The output is the generated header files;
 * The output will be compared to already checked in golden files.
 *
 * ## How to create a new test
 * Every test has a corresponding 'root' directory.
 * All directories are found in `backend.native/functionalTest/testData/objcexport`.
 *
 * 1) Create new root directory (e.g. myTest) in testData/objcexport
 * 2) Place kotlin files into the directory e.g. testData/objcexport/myTest/Foo.kt
 * 3) Create a test function and call ` doTest(objCExportTestDataDir.resolve("myTest"))`
 * 4) The first invocation will fail the test, but generates the header that can be checked in (if sufficient)
 */
class Fe10ObjCExportHeaderGeneratorTest : AbstractFE10ObjCExportHeaderGeneratorTest() {
    @Test
    fun `test - simpleClass`() {
        doTest(objCExportTestDataDir.resolve("simpleClass"))
    }

    @Test
    fun `test - simpleInterface`() {
        doTest(objCExportTestDataDir.resolve("simpleInterface"))
    }

    @Test
    fun `test - simpleEnumClass`() {
        doTest(objCExportTestDataDir.resolve("simpleEnumClass"))
    }

    @Test
    fun `test - simpleObject`() {
        doTest(objCExportTestDataDir.resolve("simpleObject"))
    }

    @Test
    fun `test - topLevelFunction`() {
        doTest(objCExportTestDataDir.resolve("topLevelFunction"))
    }

    @Test
    fun `test - topLevelProperty`() {
        doTest(objCExportTestDataDir.resolve("topLevelProperty"))
    }

    @Test
    fun `test - sameClassNameInDifferentPackage`() {
        doTest(objCExportTestDataDir.resolve("sameClassNameInDifferentPackage"))
    }

    @Test
    fun `test - samePropertyAndFunctionName`() {
        doTest(objCExportTestDataDir.resolve("samePropertyAndFunctionName"))
    }

    @Test
    fun `test - classImplementingInterface`() {
        doTest(objCExportTestDataDir.resolve("classImplementingInterface"))
    }

    @Test
    fun `test - interfaceImplementingInterface`() {
        doTest(objCExportTestDataDir.resolve("interfaceImplementingInterface"))
    }

    @Test
    fun `test - classWithObjCNameAnnotation`() {
        doTest(objCExportTestDataDir.resolve("classWithObjCNameAnnotation"))
    }

    @Test
    fun `test - functionWithObjCNameAnnotation`() {
        doTest(objCExportTestDataDir.resolve("functionWithObjCNameAnnotation"))
    }

    @Test
    fun `test - classWithKDoc`() {
        doTest(objCExportTestDataDir.resolve("classWithKDoc"))
    }

    @Test
    fun `test  - classWithHidesFromObjCAnnotation`() {
        doTest(objCExportTestDataDir.resolve("classWithHidesFromObjCAnnotation"))
    }

    @Test
    fun `test - functionWithThrowsAnnotation`() {
        doTest(objCExportTestDataDir.resolve("functionWithThrowsAnnotation"))
    }

    @Test
    fun `test - functionWithErrorType`() {
        doTest(objCExportTestDataDir.resolve("functionWithErrorType"))
    }
}
