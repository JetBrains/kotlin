class A {
    val z by
        Delegate { 23 }
}

fun foo() {
    println(A().z)
}

class Delegate(val f: () -> Int) {
    operator fun getValue(thisRef: Any?, property: Any): Int {
        return f()
    }
}

// LINES(JS):    1 2 3 * 2 2 3 3 3 3 * 6 8 7 7 10 10 11 13 12 12
// LINES(JS_IR): 3 3 3 3 1 1 3 3 3 2 3 3 3 2 1 2 6 6 7 7 10 10 10 10 10 10 10 11 12 12 2 2 18 * 2
