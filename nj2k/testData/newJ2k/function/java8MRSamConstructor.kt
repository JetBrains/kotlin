package test

import javaApi.JFunction0
import javaApi.JFunction1
import javaApi.JFunction2
import javaApi.MethodReferenceHelperClass

internal class Test {
    fun memberFun(): Int {
        return 1
    }

    constructor(i: Int) : super() {}
    constructor() {}

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

internal class Test2
internal class Java8Class {
    private val field = Java8Class()
    private val h = MethodReferenceHelperClass()
    fun testStaticFunction() {
        val staticFunFromSameClass = JFunction0 { staticFun() }
        staticFunFromSameClass.foo()
        MethodReferenceHelperClass.staticFun0 { staticFun() }
        h.memberFun0 { staticFun() }
        val staticFunFromAnotherClass = JFunction0 { Test.staticFun() }
        staticFunFromAnotherClass.foo()
        MethodReferenceHelperClass.staticFun0 { Test.staticFun() }
        h.memberFun0 { Test.staticFun() }
    }

    fun testMemberFunctionThroughClass() {
        val memberFunFromClass = JFunction2 { obj: Java8Class -> obj.memberFun() }
        memberFunFromClass.foo(Java8Class())
        MethodReferenceHelperClass.staticFun2 { obj: Java8Class -> obj.memberFun() }
        h.memberFun2 { obj: Java8Class -> obj.memberFun() }
    }

    fun testMemberFunctionThroughObject() {
        val obj = Java8Class()
        val memberFunFromSameClass = JFunction0 { obj.memberFun() }
        memberFunFromSameClass.foo()
        MethodReferenceHelperClass.staticFun0 { obj.memberFun() }
        h.memberFun0 { obj.memberFun() }
        val anotherObj = Test()
        val memFunFromAnotherClass = JFunction0 { anotherObj.memberFun() }
        memFunFromAnotherClass.foo()
        MethodReferenceHelperClass.staticFun0 { anotherObj.memberFun() }
        h.memberFun0 { anotherObj.memberFun() }
        val memberFunThroughObj1 = JFunction0 { field.memberFun() }
        memberFunThroughObj1.foo()
        MethodReferenceHelperClass.staticFun0 { field.memberFun() }
        h.memberFun0 { field.memberFun() }
        val memberFunThroughObj2 = JFunction0 { Test.field.memberFun() }
        memberFunThroughObj2.foo()
        MethodReferenceHelperClass.staticFun0 { Test.field.memberFun() }
        h.memberFun0 { Test.field.memberFun() }
        val memberFunThroughObj3 = JFunction0 { Test.staticFun().memberFun() }
        memberFunThroughObj3.foo()
        MethodReferenceHelperClass.staticFun0 { Test.staticFun().memberFun() }
        h.memberFun0 { Test.staticFun().memberFun() }
    }

    fun testConstructor() {
        val constructorSameClass = JFunction0 { Java8Class() }
        constructorSameClass.foo()
        MethodReferenceHelperClass.staticFun0 { Java8Class() }
        h.memberFun0 { Java8Class() }
        val qualifiedConstructorSameClass = JFunction0 { Java8Class() }
        qualifiedConstructorSameClass.foo()
        MethodReferenceHelperClass.staticFun0 { Java8Class() }
        h.memberFun0 { Java8Class() }
        val constructorAnotherClass = JFunction0 { Test() }
        constructorAnotherClass.foo()
        MethodReferenceHelperClass.staticFun0 { Test() }
        h.memberFun0 { Test() }
        val constructorAnotherClassWithParam = JFunction2 { i: Int -> Test(i) }
        constructorAnotherClassWithParam.foo(1)
        MethodReferenceHelperClass.staticFun2 { i: Int -> Test(i) }
        h.memberFun2 { i: Int -> Test(i) }
        val qualifiedConstructorAnotherClass = JFunction0 { Test() }
        qualifiedConstructorAnotherClass.foo()
        MethodReferenceHelperClass.staticFun0 { Test() }
        h.memberFun0 { Test() }
        val constructorAnotherClassWithoutConstructor = JFunction0 { Test2() }
        constructorAnotherClassWithoutConstructor.foo()
        MethodReferenceHelperClass.staticFun0 { Test2() }
        h.memberFun0 { Test2() }
    }

    fun testLibraryFunctions() {
        val memberFunFromClass = JFunction2 { obj: String -> obj.length }
        memberFunFromClass.foo("str")
        Thread(Runnable { println() }).start()
        Runnable { println() }.run()
    }

    fun testOverloads() {
        val constructorWithoutParams = JFunction1 { Test.testOverloads() }
        constructorWithoutParams.foo()
        MethodReferenceHelperClass.staticFun1 { Test.testOverloads() }
        h.memberFun1 { Test.testOverloads() }
        val constructorWithParam = JFunction2 { i: Int -> Test.testOverloads(i) }
        constructorWithParam.foo(2)
        MethodReferenceHelperClass.staticFun2 { i: Int -> Test.testOverloads(i) }
        h.memberFun2 { i: Int -> Test.testOverloads(i) }
    }

    fun testGenericFunctions() {
        val emptyList = JFunction1<List<String>> { emptyList() }
        emptyList.foo()
        MethodReferenceHelperClass.staticFun1<List<String>> { emptyList() }
        h.memberFun1<List<String>> { emptyList() }
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