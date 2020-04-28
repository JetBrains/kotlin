package com.jetbrains.kotlin.structuralsearch

class KotlinSSClassTest : KotlinSSTest() {
    override fun getBasePath() = "class"

    fun testClass() { doTest("class A") }

    fun testInterface() { doTest("interface A") }

    fun testClassWithAbstractModifier() { doTest("abstract class A") }

    fun testClassWithInnerModifier() { doTest("inner class B") }

    fun testClassWithOpenModifier() { doTest("open class A") }

    fun testClassWithPublicModifier() { doTest("public class A") }

    fun testClassWithInternalModifier() { doTest("internal class A") }

    fun testClassWithPrivateModifier() { doTest("private class A") }

    fun testClassWithoutAbstractModifier() { doTest("abstract class A") }

    fun testClassWithoutInnerModifier() { doTest("inner class B") }

    fun testClassWithoutOpenModifier() { doTest("open class A") }

    fun testClassWithoutPublicModifier() { doTest("public class A") }

    fun testClassWithoutInternalModifier() { doTest("internal class A") }

    fun testClassWithoutPrivateModifier() { doTest("private class A") }

    fun testClassWithProperty() {
        doTest(
            """
            class '_a {
                lateinit var 'b
            }
            """
        )
    }

    fun testClassWithVarIdentifier() { doTest("class '_:[regex( Foo.* )]") }

    fun testTwoClasses() {
        doTest(
            """
            class '_a:[regex( Foo(1)* )]
            class '_b:[regex( Bar(1)* )]
            """
        )
    }


}