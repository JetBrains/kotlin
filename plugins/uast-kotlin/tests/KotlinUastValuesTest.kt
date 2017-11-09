package org.jetbrains.uast.test.kotlin

import org.junit.Test

class KotlinUastValuesTest : AbstractKotlinValuesTest() {

    @Test fun testAssertion() = doTest("Assertion")

    @Test fun testIn() = doTest("In")

    @Test fun testLocalDeclarations() = doTest("LocalDeclarations")

    @Test fun testSimple() = doTest("Simple")
}