package smartStepIntoWithDelegates

interface A {
    fun foo(): String
}

class AA : A {
    override fun foo(): String = "AA"
}

class B(a: A) : A by a {
    override fun foo(): String = "B"
}

class C(b: B) : A by b

class D(a: A) : A by a

fun test1() {
    val c: C = C(B(AA()))
    // SMART_STEP_INTO_BY_INDEX: 1
    // RESUME: 1
    //Breakpoint!
    c.foo()                                  // 12
}

fun test2() {
    val a: A = C(B(AA()))
    // SMART_STEP_INTO_BY_INDEX: 1
    // RESUME: 1
    //Breakpoint!
    a.foo()                                  // 12
}

fun test3() {
    val d: D = D(AA())
    // SMART_STEP_INTO_BY_INDEX: 1
    // RESUME: 1
    //Breakpoint!
    d.foo()                                  // 8
}

fun test4() {
    val aa: A = AA()
    val c: B = B(AA())
    // SMART_STEP_INTO_BY_INDEX: 1
    // RESUME: 1
    //Breakpoint!
    aa.foo() + c.foo()                        // 8
}

fun test5() {
    val aa: A = AA()
    val c: B = B(AA())
    // SMART_STEP_INTO_BY_INDEX: 2
    // RESUME: 1
    //Breakpoint!
    aa.foo() + c.foo()                        // 12 (Shouldn't stop at B.foo() even it's evaluated before C.foo())
}

fun main(args: Array<String>) {
    test1()
    test2()
    test3()
    test4()
    test5()
}