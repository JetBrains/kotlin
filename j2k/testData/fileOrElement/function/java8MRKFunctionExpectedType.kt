// ERROR: Type inference failed: Not enough information to infer parameter T in fun <T> emptyList(): List<T> Please specify it explicitly.
package test

import java.util.Collections

internal class Test {
    fun memberFun(): Int {
        return 1
    }

    companion object {
        var field = Java8Class()
        fun staticFun(): Java8Class {
            return Java8Class()
        }

        fun testOverloads(): String {
            return "1"
        }

        fun testOverloads(i: Int): String {
            return "2"
        }
    }
}

internal class Java8Class {
    private val field = Java8Class()

    fun testStaticFunction() {
        val staticFunFromSameClass = { staticFun() }
        staticFunFromSameClass.invoke()

        val staticFunFromAnotherClass = { Test.staticFun() }
        staticFunFromAnotherClass.invoke()
    }

    fun testMemberFunctionThroughClass() {
        val memberFunFromClass = { obj: Java8Class -> obj.memberFun() }
        memberFunFromClass.invoke(Java8Class())
    }

    fun testMemberFunctionThroughObject() {
        val obj = Java8Class()
        val memberFunFromSameClass = { obj.memberFun() }
        memberFunFromSameClass.invoke()

        val anotherObj = Test()
        val memFunFromAnotherClass = { anotherObj.memberFun() }
        memFunFromAnotherClass.invoke()

        val memberFunThroughObj1 = { field.memberFun() }
        memberFunThroughObj1.invoke()
        val memberFunThroughObj2 = { Test.field.memberFun() }
        memberFunThroughObj2.invoke()
        val memberFunThroughObj3 = { Test.staticFun().memberFun() }
        memberFunThroughObj3.invoke()
    }

    fun testConstructor() {
        val constructorSameClass = { Java8Class() }
        constructorSameClass.invoke()

        val qualifiedConstructorSameClass = { test.Java8Class() }
        qualifiedConstructorSameClass.invoke()

        val constructorAnotherClass = { Test() }
        constructorAnotherClass.invoke()

        val qualifiedConstructorAnotherClass = { test.Test() }
        qualifiedConstructorAnotherClass.invoke()
    }

    fun testLibraryFunctions() {
        val memberFunFromClass = { obj: String -> obj.length }
        memberFunFromClass.invoke("str")
    }

    fun testOverloads() {
        val constructorWithoutParams = { Test.testOverloads() }
        constructorWithoutParams.invoke()

        val constructorWithParam = { i: Int -> Test.testOverloads(i) }
        constructorWithParam.invoke(2)
    }

    fun testGenericFunctions() {
        val emptyList = { emptyList() }
        emptyList.invoke()
    }

    fun memberFun(): Int {
        return 1
    }

    companion object {

        fun staticFun(): Int {
            return 1
        }
    }
}