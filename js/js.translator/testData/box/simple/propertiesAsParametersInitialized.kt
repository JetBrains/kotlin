package foo

class A(var b: Int, var a: String) {

}

fun box(): String {
    val c = A(2, "2")
    return if (c.b == 2 && c.a == "2") "OK" else "fail"
}