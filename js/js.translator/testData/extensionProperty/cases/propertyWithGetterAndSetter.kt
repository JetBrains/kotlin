package foo

class Test() {
    var a = 0
}

var Test.b: Int
    get() = a * 3
    set(c: Int) {
        a = c - 1
    }

val Test.d: Int = 44

fun box(): Boolean {
    val c = Test()
    if (c.a != 0) return false;
    if (c.b != 0) return false;
    c.a = 3;
    if (c.b != 9) return false;
    c.b = 10;
    if (c.a != 9) return false;
    if (c.b != 27) return false;
    if (c.d != 44) return false;
    return true;
}