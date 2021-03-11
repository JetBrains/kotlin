class Bar {
    val aa = "Some"
}

fun foo() {
    val bar = Bar()
    val y = """$bar.
a<caret>"""
}

// NO_LOOKUP