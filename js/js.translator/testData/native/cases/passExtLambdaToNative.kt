package foo

@native
class A(val v: String)

@native
fun bar(a: A, extLambda: A.(Int, String) -> String): String = noImpl

fun box(): String {
    val a = A("test")

    val r = bar(a) { i, s -> "${this.v} $i $s"}
    if (r != "test 4 boo") return r

    return "OK"
}