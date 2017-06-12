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

// LINES: 2 3 2 * 3 * 7 10 12
