package foo

data class A(private val a: Int, protected val b: String) {
    fun c1(): Int = component1()
    fun c2(): String = component2()
}

fun box(): String {
    val t = A(123, "abba")

    val a = t.c1()
    if (a != 123) return "a /*$a*/ != 123"

    val b = t.c2()
    if (b != "abba") return "b /*$b*/ != abba"

    return "OK"
}
