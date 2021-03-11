package org.jetbrains.kotlin.idea.structuralsearch.search

import org.jetbrains.kotlin.idea.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSDestructuringDeclarationTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "destructuringDeclaration"

    fun testDataClass() { doTest("val ('_, '_, '_) = '_") }

    fun testLoop() { doTest("for (('_, '_) in '_) { '_* }") }

    fun testVariable() { doTest("{ '_ -> '_* }") }

    fun testVariableFor() { doTest("for ('_ in '_) { '_* }") }
}