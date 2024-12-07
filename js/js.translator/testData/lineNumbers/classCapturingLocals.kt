class A

fun A.foo() {
    val x = baz()
    class B {
        fun bar() {
            println(this@foo)
            println("A.B.bar: $x")
        }
    }
    println("A.foo: $x")
    B().bar()
}

fun baz() = 23

// LINES(JS_IR): 1 1 3 3 4 11 11 12 12 15 15 15 15 5 3 4 5 * 6 7 7 8 8
