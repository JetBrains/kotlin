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

// LINES: 5 5 7 8 * 4 11 12 15