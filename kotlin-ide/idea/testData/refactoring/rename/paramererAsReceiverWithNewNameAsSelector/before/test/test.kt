interface I {
    fun some()
}

fun f(/*rename*/o: I) {
    println(o)
    o.some()
}