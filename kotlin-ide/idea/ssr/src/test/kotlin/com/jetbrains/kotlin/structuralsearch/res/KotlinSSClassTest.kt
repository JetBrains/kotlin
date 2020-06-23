package com.jetbrains.kotlin.structuralsearch.res

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSClassTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "class"

    fun testClass() { doTest("class A") }

    fun testClassDoubleInheritance() { doTest("class '_ : A, B()") }

    fun testClassSingleInheritance() { doTest("class '_ : A") }

    fun testClassExtendsParam() { doTest("class '_ : '_('_, '_)") }

    fun testClassDelegation() { doTest("class '_(b: B) : A by b") }

    fun testClassConstrPrim() { doTest("class A(b: Int, c: String)") }

    fun testClassConstrPrimModifier() { doTest("class '_ private constructor()") }

    fun testClassConstrPrimDiffType() { doTest("class A(b: Int, c: String)") }

    fun testClassConstrPrimDefaultValue() { doTest("class '_(b: Int, c: String = \"a\")") }

    fun testClassConstrSec() {
        doTest(
            """
            class '_(val b: Int) {
                var c: String? = null

                constructor(b: Int, c: String) : this(b) {
                    this.c = c
                }
            }
            """
        )
    }

    fun testClassConstrSecModifier() {
        doTest(
            """
            class '_(val b: Int) {
                var c: String? = null

                private constructor(b: Int, c: String) : this(b) {
                    this.c = c
                }
            }
            """
        )
    }

    fun testClassTypeParam() { doTest("class '_<T, R>(val a: T, val b: R, val c: T)") }

    fun testClassTypeParamExtBound() { doTest("class '_<'_, '_ : List<*>>(val a: T, val b: R, val c: T)") }

    fun testClassTypeParamProjection() { doTest("class '_<'_T : Comparable<'_T>>") }

    fun testClassTypeParamVariance() { doTest("class '_<out V>") }

    fun testInterface() { doTest("interface '_") }

    fun testDataClass() { doTest("data class '_") }

    fun testEnumClass() { doTest("enum class '_") }

    fun testInnerClass() { doTest("inner class '_") }

    fun testSealedClass() { doTest("sealed class '_") }

    fun testClassAbstractModifier() { doTest("abstract class '_") }

    fun testClassOpenModifier() { doTest("open class '_") }

    fun testClassPublicModifier() { doTest("public class '_") }

    fun testClassInternalModifier() { doTest("internal class '_") }

    fun testClassPrivateModifier() { doTest("private class '_") }

    fun testClassInit() {
        doTest(
            """
            class '_ {
                init {
                    val a = 3
                    println(a)
                }
            }
            """
        )
    }

    fun testClassProperty() {
        doTest(
            """
            class '_ {
                lateinit var 'a
            }
            """
        )
    }

    fun testClassVarIdentifier() { doTest("class '_:[regex( Foo.* )]") }

    fun testTwoClasses() {
        doTest(
            """
            class '_a:[regex( Foo(1)* )]
            class '_b:[regex( Bar(1)* )]
            """
        )
    }

    fun testClassOptionalVars() {
        doTest(
            """
            class '_Class {  
                var '_Field* = '_Init?
            }
            """
        )
    }
}