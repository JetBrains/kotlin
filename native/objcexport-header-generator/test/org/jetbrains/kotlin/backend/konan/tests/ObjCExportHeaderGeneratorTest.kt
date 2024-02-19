/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.tests

import org.jetbrains.kotlin.backend.konan.testUtils.HeaderGenerator
import org.jetbrains.kotlin.backend.konan.testUtils.HeaderGenerator.Configuration
import org.jetbrains.kotlin.backend.konan.testUtils.TodoAnalysisApi
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
class ObjCExportHeaderGeneratorTest(private val generator: HeaderGenerator) {

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
    @TodoAnalysisApi
    fun `test - sameClassNameInDifferentPackage`() {
        doTest(headersTestDataDir.resolve("sameClassNameInDifferentPackage"))
    }

    @Test
    fun `test - nestedClass`() {
        doTest(headersTestDataDir.resolve("nestedClass"))
    }

    @Test
    fun `test - nestedClassWithFrameworkName`() {
        doTest(headersTestDataDir.resolve("nestedClassWithFrameworkName"), Configuration(frameworkName = "Shared"))
    }

    @Test
    fun `test - nestedGenericClass`() {
        doTest(headersTestDataDir.resolve("nestedGenericClass"))
    }

    @Test
    fun `test - nestedInterface`() {
        doTest(headersTestDataDir.resolve("nestedInterface"))
    }

    @Test
    @TodoAnalysisApi
    fun `test - samePropertyAndFunctionName`() {
        doTest(headersTestDataDir.resolve("samePropertyAndFunctionName"))
    }

    @Test
    fun `test - classImplementingInterface`() {
        doTest(headersTestDataDir.resolve("classImplementingInterface"))
    }

    @Test
    fun `test - classExtendsAbstractClass`() {
        doTest(headersTestDataDir.resolve("classExtendsAbstractClass"))
    }

    @Test
    @TodoAnalysisApi
    fun `test - interfaceImplementingInterface`() {
        doTest(headersTestDataDir.resolve("interfaceImplementingInterface"))
    }

    @Test
    fun `test - classWithObjCNameAnnotation`() {
        doTest(headersTestDataDir.resolve("classWithObjCNameAnnotation"))
    }

    @Test
    @TodoAnalysisApi
    fun `test - functionWithObjCNameAnnotation`() {
        doTest(headersTestDataDir.resolve("functionWithObjCNameAnnotation"))
    }

    @Test
    fun `test - propertyWithObjCNameAnnotation`() {
        doTest(headersTestDataDir.resolve("propertyWithObjCNameAnnotation"))
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
    @TodoAnalysisApi
    fun `test - functionWithThrowsAnnotation`() {
        doTest(headersTestDataDir.resolve("functionWithThrowsAnnotation"))
    }

    @Test
    fun `test - functionWithErrorType`() {
        doTest(headersTestDataDir.resolve("functionWithErrorType"))
    }

    @Test
    @TodoAnalysisApi
    fun `test - functionWithErrorTypeAndFrameworkName`() {
        doTest(headersTestDataDir.resolve("functionWithErrorTypeAndFrameworkName"), Configuration(frameworkName = "shared"))
    }

    @Test
    @TodoAnalysisApi
    fun `test - kdocWithBlockTags`() {
        doTest(headersTestDataDir.resolve("kdocWithBlockTags"))
    }

    @Test
    fun `test - classWithMustBeDocumentedAnnotation`() {
        doTest(headersTestDataDir.resolve("classWithMustBeDocumentedAnnotation"))
    }

    @Test
    fun `test - interfaceWithMustBeDocumentedAnnotation`() {
        doTest(headersTestDataDir.resolve("interfaceWithMustBeDocumentedAnnotation"))
    }

    @Test
    fun `test - functionWithMustBeDocumentedAnnotation`() {
        doTest(headersTestDataDir.resolve("functionWithMustBeDocumentedAnnotation"))
    }

    @Test
    @TodoAnalysisApi
    fun `test - parameterWithMustBeDocumentedAnnotation`() {
        doTest(headersTestDataDir.resolve("parameterWithMustBeDocumentedAnnotation"))
    }

    @Test
    @TodoAnalysisApi
    fun `test - receiverWithMustBeDocumentedAnnotation`() {
        doTest(headersTestDataDir.resolve("receiverWithMustBeDocumentedAnnotation"))
    }

    @Test
    @TodoAnalysisApi
    fun `test - dispatchAndExtensionReceiverWithMustBeDocumentedAnnotation`() {
        doTest(headersTestDataDir.resolve("dispatchAndExtensionReceiverWithMustBeDocumentedAnnotation"))
    }

    @Test
    fun `test - classWithUnresolvedSuperType`() {
        doTest(headersTestDataDir.resolve("classWithUnresolvedSuperType"))
    }

    @Test
    fun `test - classWithUnresolvedSuperTypeGenerics`() {
        doTest(headersTestDataDir.resolve("classWithUnresolvedSuperTypeGenerics"))
    }

    @Test
    fun `test - topLevelFunctionWithNumberReturn`() {
        doTest(headersTestDataDir.resolve("topLevelFunctionWithNumberReturn"))
    }

    @Test
    fun `test - classWithManyMembers`() {
        doTest(headersTestDataDir.resolve("classWithManyMembers"))
    }

    @Test
    fun `test - manyClassesAndInterfaces`() {
        doTest(headersTestDataDir.resolve("manyClassesAndInterfaces"))
    }

    @Test
    fun `test - classReferencingOtherClassAsReturnType`() {
        doTest(headersTestDataDir.resolve("classReferencingOtherClassAsReturnType"))
    }

    /**
     * - IntIterator has unwanted 'hasNext' exposed
     * - IntIterator's next method returns int32_t instead of expected Int *
     */
    @Test
    @TodoAnalysisApi
    fun `test - classReferencingDependencyClassAsReturnType`() {
        doTest(headersTestDataDir.resolve("classReferencingDependencyClassAsReturnType"))
    }

    @Test
    fun `test - interfaceReferencingOtherInterfaceAsReturnType`() {
        doTest(headersTestDataDir.resolve("interfaceReferencingOtherInterfaceAsReturnType"))
    }

    @Test
    fun `test - interfaceImplementingInterfaceOrder`() {
        doTest(headersTestDataDir.resolve("interfaceImplementingInterfaceOrder"))
    }

    /**
     * Extension functions aren't supported KT-65630
     */
    @Test
    @TodoAnalysisApi
    fun `test - extensionFunctions`() {
        doTest(headersTestDataDir.resolve("extensionFunctions"))
    }

    @Test
    fun `test - classWithGenerics`() {
        doTest(headersTestDataDir.resolve("classWithGenerics"))
    }

    /**
     * - init method missing
     * - 'new constructor' missing
     */
    @Test
    @TodoAnalysisApi
    fun `test - objectWithGenericSuperclass`() {
        doTest(headersTestDataDir.resolve("objectWithGenericSuperclass"))
    }

    @Test
    fun `test - since version annotation`() {
        doTest(headersTestDataDir.resolve("sinceVersionAnnotation"))
    }

    /**
     * - requires mangling
     */
    @TodoAnalysisApi
    @Test
    fun `test - constructors`() {
        doTest(headersTestDataDir.resolve("constructors"))
    }

    @Test
    fun `test - companion`() {
        doTest(headersTestDataDir.resolve("companion"))
    }

    @Test
    fun `test - anonymous functions`() {
        doTest(headersTestDataDir.resolve("anonymousFunctions"))
    }

    @Test
    fun `test - sam interface`() {
        doTest(headersTestDataDir.resolve("samInterface"))
    }

    /**
     * Requires some fixes: KT-65800
     */
    @Test
    @TodoAnalysisApi
    fun `test - simple data class`() {
        doTest(headersTestDataDir.resolve("simpleDataClass"))
    }

    @Test
    fun `test - special function names`() {
        doTest(headersTestDataDir.resolve("specialFunctionNames"))
    }

    private fun doTest(root: File, configuration: Configuration = Configuration()) {
        if (!root.isDirectory) fail("Expected ${root.absolutePath} to be directory")
        val generatedHeaders = generator.generateHeaders(root, configuration).toString()
        KotlinTestUtils.assertEqualsToFile(root.resolve("!${root.nameWithoutExtension}.h"), generatedHeaders)
    }
}
