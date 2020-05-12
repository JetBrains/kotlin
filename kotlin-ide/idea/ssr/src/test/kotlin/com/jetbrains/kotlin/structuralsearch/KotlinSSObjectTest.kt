package com.jetbrains.kotlin.structuralsearch

class KotlinSSObjectTest : KotlinSSTest() {
    override fun getBasePath() = "objectDeclaration"

    fun testObject() { doTest("object '_") }

    fun testCompanionObject() { doTest("object A") }

    fun testNestedObject() { doTest("object B") }
}