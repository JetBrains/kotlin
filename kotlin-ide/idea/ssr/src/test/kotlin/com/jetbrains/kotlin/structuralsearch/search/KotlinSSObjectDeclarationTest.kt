package com.jetbrains.kotlin.structuralsearch.search

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSObjectDeclarationTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "objectDeclaration"

    fun testObject() { doTest("object '_") }

    fun testCompanionObject() { doTest("object A") }

    fun testNestedObject() { doTest("object B") }

    fun testNamedCompanionObject() {
        doTest(
            """class '_ {
                    companion object Foo { }
                }""".trimMargin()
        )
    }

    fun testNestedNamedCompanionObject() {
        doTest(
            """class '_ {
                    companion object Foo { }
                }""".trimMargin()
        )
    }
}