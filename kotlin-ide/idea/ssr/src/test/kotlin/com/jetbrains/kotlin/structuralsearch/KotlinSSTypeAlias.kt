package com.jetbrains.kotlin.structuralsearch

class KotlinSSTypeAlias : KotlinSSTest() {
    override fun getBasePath() = "typeAlias"

    fun testTypeAlias() { doTest("typealias '_ = Int") }
}