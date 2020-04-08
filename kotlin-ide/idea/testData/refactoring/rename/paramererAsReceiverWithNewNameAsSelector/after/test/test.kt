interface I {
    fun some()
}

fun f(some: I) {
    println(some)
    some.some()
}