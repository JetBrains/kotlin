package com.jetbrains.kotlin.structuralsearch

class KotlinSSTypeAliasTest : KotlinSSTest() {
    override fun getBasePath() = "typeAlias"

    fun testTypeAlias() { doTest("typealias '_ = Int") }
}