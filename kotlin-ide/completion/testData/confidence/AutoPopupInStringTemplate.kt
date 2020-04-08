class Bar {
    val aa = "Some"
}

fun foo() {
    val bar = Bar()
    val y = "$bar.<caret>"
}

// ELEMENT: aa