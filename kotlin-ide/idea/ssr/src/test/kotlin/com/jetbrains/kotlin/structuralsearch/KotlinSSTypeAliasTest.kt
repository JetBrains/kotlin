package com.jetbrains.kotlin.structuralsearch

class KotlinSSTypeAliasTest : KotlinSSTest() {
    override fun getBasePath(): String = "typeAlias"

    fun testTypeAlias() { doTest("typealias '_ = Int") }
}