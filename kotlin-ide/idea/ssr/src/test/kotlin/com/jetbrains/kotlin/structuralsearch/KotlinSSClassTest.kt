package com.jetbrains.kotlin.structuralsearch

class KotlinSSClassTest : KotlinSSTest() {
    override fun getBasePath() = "class"

    fun testClass() { doTest("class Foo") }

    fun testClassWithVarIdentifier() { doTest("class '_:[regex( Foo.* )]") }

    fun testClassWithOpenModifier() { doTest("open class Foo") }

    fun testTwoClasses() { doTest("""
        class '_a:[regex( Foo(1)* )]
        class '_b:[regex( Bar(1)* )]
    """.trimIndent()) }

    fun testClassWithProperty() { doTest("""
        class '_a {
            lateinit var 'b
        }
    """.trimIndent()) }
}