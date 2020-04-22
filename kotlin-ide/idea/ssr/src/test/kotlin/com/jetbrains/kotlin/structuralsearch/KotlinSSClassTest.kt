package com.jetbrains.kotlin.structuralsearch

class KotlinSSClassTest : KotlinSSTest() {
    override fun getBasePath() = "class"

    fun testClass() { doTest("class Foo") }

    fun testClassWithVarIdentifier() { doTest("class '_:[regex( Foo.* )]") }

    fun testClassWithOpenModifier() { doTest("open class Foo") }
}