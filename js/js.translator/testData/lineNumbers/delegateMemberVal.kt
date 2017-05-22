class A {
    val z by
            lazy { 23 }
}

fun foo() {
    println(A().z)
}

// LINES: 2 3 * 2 * 3 * 7
