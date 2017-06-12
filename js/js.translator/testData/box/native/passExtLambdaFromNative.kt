// EXPECTED_REACHABLE_NODES: 492
package foo

internal external class A(v: String) {
    val v: String
}

internal class B {
    fun bar(a: A, extLambda: A.(Int, String) -> String): String = a.extLambda(7, "_rr_")
}


internal external fun nativeBox(b: B): String = definedExternally

fun box(): String {
    val r = nativeBox(B())
    if (r != "foo_rr_7") return r

    return "OK"
}