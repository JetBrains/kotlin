package com.jetbrains.kotlin.structuralsearch

class KotlinSSDestructuringDeclarationTest : KotlinSSTest() {
    override fun getBasePath() = "destructuringDeclaration"

    fun testDataClass() { doTest("val ('_, '_, '_) = '_") }

    fun testLoop() { doTest("for (('_, '_) in '_) { '_* }") }

}