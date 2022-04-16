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

// LINES(JS):    1 * 5 5 5 6 9 7 7 8 8 * 3 13 4 4 11 11 12 12 15 15 15
// LINES(JS_IR):                              4 4 11 11 12 12 *  15 15 * 7 7 8 8
