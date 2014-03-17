package foo

native
class A(val v: String)

class B {
    fun bar(a: A, extLambda: A.(Int, String) -> String): String = a.extLambda(7, "_rr_")
}

native
fun nativeBox(b: B): String = noImpl

fun box(): String {
    val r = nativeBox(B())
    if (r != "foo_rr_7") return r

    return "OK"
}