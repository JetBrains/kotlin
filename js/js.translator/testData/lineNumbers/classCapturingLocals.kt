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

// LINES: 1 * 5 5 5 9 7 7 8 8 * 13 4 4 11 11 12 12 15 15 15