package foo

class A(var b: Int, var a: String) {

}

fun box(): Boolean {
    val c = A(1, "1")
    c.b = 2
    c.a = "2"
    return (c.b == 2 && c.a == "2")
}