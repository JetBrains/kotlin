package com.jetbrains.kotlin.structuralsearch

class KotlinSSCallableReferenceTest : KotlinSSTest() {
    override fun getBasePath(): String = "callableReference"

    fun testCallableReference() { doTest("::'_") }

    fun testExtensionFun() { doTest("List<Int>::'_") }
}