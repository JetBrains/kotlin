package foo

@native
internal class A(val v: String)

internal class B {
    fun bar(a: A, extLambda: A.(Int, String) -> String): String = a.extLambda(7, "_rr_")
}

@native
internal fun nativeBox(b: B): String = noImpl

fun box(): String {
    val r = nativeBox(B())
    if (r != "foo_rr_7") return r

    return "OK"
}