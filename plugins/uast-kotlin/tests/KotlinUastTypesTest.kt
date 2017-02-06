package org.jetbrains.uast.test.kotlin

import org.junit.Test

class KotlinUastTypesTest : AbstractKotlinTypesTest() {
    @Test fun testLocalDeclarations() = doTest("LocalDeclarations")

    @Test fun testUnexpectedContainerException() = doTest("UnexpectedContainerException")
}