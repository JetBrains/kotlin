// ERROR: Type inference failed: Not enough information to infer parameter T in fun <T : kotlin.Any!> emptyList(): kotlin.(Mutable)List<T!>! Please specify it explicitly.
package test

import java.util.Collections

class Test {
    public fun memberFun(): Int {
        return 1
    }

    companion object {
        public var field: Java8Class = Java8Class()
        public fun staticFun(): Java8Class {
            return Java8Class()
        }

        public fun testOverloads(): String {
            return "1"
        }

        public fun testOverloads(i: Int): String {
            return "2"
        }
    }
}

class Java8Class {
    private val field = Java8Class()

    public fun testStaticFunction() {
        val staticFunFromSameClass = { staticFun() }
        staticFunFromSameClass.invoke()

        val staticFunFromAnotherClass = { Test.staticFun() }
        staticFunFromAnotherClass.invoke()
    }

    public fun testMemberFunctionThroughClass() {
        val memberFunFromClass = { obj: Java8Class -> obj.memberFun() }
        memberFunFromClass.invoke(Java8Class())
    }

    public fun testMemberFunctionThroughObject() {
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

    public fun testConstructor() {
        val constructorSameClass = { Java8Class() }
        constructorSameClass.invoke()

        val qualifiedConstructorSameClass = { test.Java8Class() }
        qualifiedConstructorSameClass.invoke()

        val constructorAnotherClass = { Test() }
        constructorAnotherClass.invoke()

        val qualifiedConstructorAnotherClass = { test.Test() }
        qualifiedConstructorAnotherClass.invoke()
    }

    public fun testLibraryFunctions() {
        val memberFunFromClass = { obj: String -> obj.length() }
        memberFunFromClass.invoke("str")
    }

    public fun testOverloads() {
        val constructorWithoutParams = { Test.testOverloads() }
        constructorWithoutParams.invoke()

        val constructorWithParam = { i: Int -> Test.testOverloads(i) }
        constructorWithParam.invoke(2)
    }

    public fun testGenericFunctions() {
        val emptyList = { Collections.emptyList() }
        emptyList.invoke()
    }

    public fun memberFun(): Int {
        return 1
    }

    companion object {

        public fun staticFun(): Int {
            return 1
        }
    }
}