fun box(a: C, x: dynamic) {
    println(a
            .foo()
            .bar())

    println(a
            .baz
            .boo)
}

class C {
    fun foo(): dynamic = null

    val baz: dynamic get() = null
}

// LINES: 1 9 2 4 3 2 6 8 7 6 11 12 12 12 14 14 14