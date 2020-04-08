package constructors

class Derived2(): Base(1) {

    // constructor with body
    // EXPRESSION: p
    // RESULT: 1: I
    //FunctionBreakpoint!
    constructor(p: Int): this() {
        // EXPRESSION: p + 1
        // RESULT: 2: I
        //Breakpoint!
        val a = 1
    }

    // constructor without body
    // EXPRESSION: p1 + p2
    // RESULT: 2: I
    //FunctionBreakpoint!
    constructor(p1: Int, p2: Int): this()
}

// EXPRESSION: i1
// RESULT: 1: I
class Derived1(
        i1: Int
        //Breakpoint!
): Base(i1)

open class Base(i: Int)

fun main(args: Array<String>) {
    Derived2(1)
    Derived2(1, 1)

    Derived1(1)

    A()
    AA()
    B()
    C(1)
    D()
    E(1)
    F("foo")
}

// EXPRESSION: 1 + 1
// RESULT: 2: I
//FunctionBreakpoint!
class A

// EXPRESSION: 1 + 3
// RESULT: 4: I
//FunctionBreakpoint!
class AA {

}

// EXPRESSION: 1 + 2
// RESULT: 3: I
//FunctionBreakpoint!
class B()

// EXPRESSION: a
// RESULT: 1: I
//FunctionBreakpoint!
class C(val a: Int)

class D {
    // EXPRESSION: 1 + 3
    // RESULT: 4: I
    //FunctionBreakpoint!
    constructor()
}
class E {
    // EXPRESSION: i
    // RESULT: 1: I
    //FunctionBreakpoint!
    constructor(i: Int)
}

// EXPRESSION: a
// RESULT: "foo": Ljava/lang/String;
//FunctionBreakpoint!
class F(val a: String)