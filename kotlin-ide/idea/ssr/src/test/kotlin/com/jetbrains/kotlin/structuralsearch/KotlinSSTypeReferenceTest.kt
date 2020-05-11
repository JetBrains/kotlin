package com.jetbrains.kotlin.structuralsearch

class KotlinSSTypeReferenceTest : KotlinSSTest() {
    override fun getBasePath() = "typeReference"

    fun testAny() { doTest("fun '_('_ : '_) { '_* }") }

    fun testFqType() { doTest("fun '_('_ : kotlin.Int) { '_* }") }

    fun testFunctionType() { doTest("fun '_('_ : ('_) -> ('_)) { '_* }") }

    fun testNullableType() { doTest("fun '_('_ : '_ ?) { '_* }") }
}