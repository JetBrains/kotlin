package com.jetbrains.kotlin.structuralsearch.res

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSAnnotationTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "annotation"

    fun testAnnotation() { doTest("@Foo") }

    fun testClassAnnotation() { doTest("@A class '_") }

    fun testClassAnnotations() { doTest("@'_Annotation{0,2} class '_Name") }

    fun testClass2Annotations() { doTest("@'_Annotation{2,100} class '_Name") }

    fun testFunAnnotation() { doTest("@A fun '_() { println(0) }") }

    fun testClassAnnotationArgs() { doTest("@A(0) class '_()") }
}