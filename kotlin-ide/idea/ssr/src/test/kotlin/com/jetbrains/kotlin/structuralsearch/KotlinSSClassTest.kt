package com.jetbrains.kotlin.structuralsearch

class KotlinSSClassTest : KotlinSSTest() {
    override fun getBasePath() = "class"

    fun testClass() { doTest("class A") }

    fun testClassConstr() { doTest("class A(b: Int, c: String)") }

    fun testClassConstrDiffType() { doTest("class A(b: Int, c: String)") }

    fun testClassConstrDefaultValue() { doTest("class '_(b: Int, c: String = \"a\")") }

    fun testClassTypeArgs() { doTest("class '_<T, R>(val a: T, val b: R, val c: T)") }

    fun testClassTypeArgsExtBound() { doTest("class '_<'_, '_ : List<*>>(val a: T, val b: R, val c: T)") }

    fun testClassTypeArgsVariance() { doTest("class '_<out V>") }

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
            class '_a {
                lateinit var 'b
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
}