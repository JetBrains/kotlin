class C {
    val bar: Int
}

fun foo(c: C) {
    val v = c.<caret>getBar(0)
}

// ELEMENT: bar
// CHAR: '\t'
