package severalFunLiterals

fun main(args: Array<String>) {
    val myClass = MyClass()
    //Breakpoint! (lambdaOrdinal = -1)
    myClass.f1 { test() }.f2 {
        test()
    }
}

class MyClass {
    fun f1(f1Param: () -> Unit): MyClass {
        f1Param()
        return this
    }

    fun f2(f2Param: () -> Unit): MyClass {
        f2Param()
        return this
    }
}

fun test() {}

// SMART_STEP_INTO_BY_INDEX: 4
