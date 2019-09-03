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

// LINES: 1 2 3 * 2 2 3 3 3 3 * 6 8 7 7 10 10 11 13 12 12
