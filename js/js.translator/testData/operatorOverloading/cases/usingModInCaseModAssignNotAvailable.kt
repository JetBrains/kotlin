package foo

class A() {
    var p = "yeah"
    operator fun mod(other: A): A {
        return A();
    }
}

fun box(): Boolean {
    var c = A()
    val d = c;
    c %= A();
    return (c != d) && (c.p == "yeah")
}