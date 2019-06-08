package test

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
        val staticFunFromSameClass: Function0<*> = { staticFun() }
        staticFunFromSameClass.invoke()
        val staticFunFromAnotherClass: Function0<*> = { Test.staticFun() }
        staticFunFromAnotherClass.invoke()
    }

    fun testMemberFunctionThroughClass() {
        val memberFunFromClass = { obj: Java8Class -> obj.memberFun() }
        memberFunFromClass.invoke(Java8Class())
    }

    fun testMemberFunctionThroughObject() {
        val obj = Java8Class()
        val memberFunFromSameClass: Function0<*> = { obj.memberFun() }
        memberFunFromSameClass.invoke()

        val anotherObj = Test()
        val memFunFromAnotherClass: Function0<*> = { anotherObj.memberFun() }
        memFunFromAnotherClass.invoke()
        val memberFunThroughObj1: Function0<*> = { field.memberFun() }
        memberFunThroughObj1.invoke()
        val memberFunThroughObj2: Function0<*> = { Test.field.memberFun() }
        memberFunThroughObj2.invoke()
        val memberFunThroughObj3: Function0<*> = { Test.staticFun().memberFun() }
        memberFunThroughObj3.invoke()
    }

    fun testConstructor() {
        val constructorSameClass: Function0<*> = { Java8Class() }
        constructorSameClass.invoke()
        val qualifiedConstructorSameClass: Function0<*> = { Java8Class() }
        qualifiedConstructorSameClass.invoke()
        val constructorAnotherClass: Function0<*> = { Test() }
        constructorAnotherClass.invoke()
        val qualifiedConstructorAnotherClass: Function0<*> = { Test() }
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
        constructorWithParam.invoke(2) + 42
    }

    fun testGenericFunctions() {
        val emptyList: Function0<List<String>> = { emptyList() }
        val list = emptyList.invoke()
        list[0]
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