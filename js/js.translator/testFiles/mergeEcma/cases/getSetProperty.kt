package foo

class A {
    val a: Int
        get() {
            return 2
        }

    var b: Int = 1
        get() {
            return $b + 1;
        }
        set(value: Int) {
            $b = value;
        }
}

fun box(): String {
    val a = A()
    if (a.a != 2) return "a.a != 2, it: ${a.a}"

    if (a.b != 2) return "a.b != 2, it: ${a.b}"
    a.b = 3

    if (a.b != 4) return "a.b != 4, it: ${a.b}"
    return "OK"
}