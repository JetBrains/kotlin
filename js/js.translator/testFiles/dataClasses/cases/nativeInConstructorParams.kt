package foo

data class AA(a: Int, val b: Int, c: Int)

data class A(native("foo") val a: Int)

fun box(): String {
    val t = A(123)

    if (t.a != 123) return "t.a /*${t.a}*/ != 123"

    val (a) = t
    if (a != 123) return "a /*$a*/ != 123"

    return "OK"
}
