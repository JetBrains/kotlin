package org.jetbrains.uast.test.kotlin

import org.junit.Test

class KotlinUastTypesTest : AbstractKotlinTypesTest() {
    @Test fun testLocalDeclarations() = doTest("LocalDeclarations")

    @Test fun testUnexpectedContainerException() = doTest("UnexpectedContainerException")

    @Test fun testCycleInTypeParameters() = doTest("CycleInTypeParameters")

    @Test fun testEa101715() = doTest("ea101715")

    @Test fun testStringTemplate() = doTest("StringTemplate")

    @Test fun testStringTemplateComplex() = doTest("StringTemplateComplex")

    @Test fun testInferenceInsideUnresolvedConstructor() = doTest("InferenceInsideUnresolvedConstructor")

    @Test fun testInnerNonFixedTypeVariable() = doTest("InnerNonFixedTypeVariable")
}