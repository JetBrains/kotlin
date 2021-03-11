package functionBreakpoints

//FunctionBreakpoint!
class A

//FunctionBreakpoint!
class B()

//FunctionBreakpoint!
class C {}

//FunctionBreakpoint!
class D(val a: Int)

//FunctionBreakpoint!
class E(
    val a: Int
)

class F(
    //Breakpoint!
    val a: Int
)

class G {
    //FunctionBreakpoint!
    constructor(a: Int) {}

    //FunctionBreakpoint!
    constructor(a: String) {}
}

//FunctionBreakpoint!
class H(val a: String) {
    //FunctionBreakpoint!
    constructor(a: Int) : this("f")
}

//FunctionBreakpoint!
class K {
    //FunctionBreakpoint!
    fun a() {}

    //FunctionBreakpoint!
    fun b() {
    }

    //FunctionBreakpoint!
    fun c(a: Int) {
        b()
    }
}

//FunctionBreakpoint!
fun topLevel1() {}

//FunctionBreakpoint!
fun topLevel2(
    a: Int
) {}

//FunctionBreakpoint!
fun topLevel3(a: Int = foo()) {}

//FunctionBreakpoint!
fun foo() = 3

class L {
    val a: Int
        //FunctionBreakpoint!
        get() = 1

    var b: Int
        //FunctionBreakpoint!
        get() = 1
        //FunctionBreakpoint!
        set(v) { topLevel2(v) }
}

fun main() {
    A()
    B()
    C()
    D(0)
    E(0)
    F(0)
    G(0)
    G("")
    H(0)
    H("")

    val k = K()
    k.a()
    k.b()
    k.c(0)

    topLevel1()
    topLevel2(0)
    topLevel3()

    val l = L()
    l.a
    l.b
    l.b = 2
}