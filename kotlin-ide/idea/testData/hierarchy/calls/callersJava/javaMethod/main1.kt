class A {
    val x = J().foo()

    init {
        J().foo()
    }
}

val y = J().foo()

fun test(j: J) = j.foo()