package com.jetbrains.kotlin.structuralsearch

class KotlinSSObjectExpression : KotlinSSTest() {
    override fun getBasePath(): String = "objectExpression"

    fun testObject() {
        doTest(
            """
                fun '_() = object {
                    val c = 1
                }
            """
        )
    }

    fun testObjectAnyReturn() {
        doTest(
            """
                fun '_(): Any = object {
                    val c = 1
                }
            """
        )
    }

    fun testObjectAnonymous() {
        doTest(
            """
                private fun '_() = object {
                    val c = 1
                }
            """
        )
    }

    fun testObjectSuperType() {
        doTest(
            """
                fun '_() = object : MouseAdapter() { }
            """
        )
    }
}