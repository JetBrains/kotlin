package com.jetbrains.kotlin.structuralsearch

class KotlinSSLiteralTest : KotlinSSTest() {
    override fun testDataFolder() = "literal"

    fun testInteger() { doTest("1") }
}