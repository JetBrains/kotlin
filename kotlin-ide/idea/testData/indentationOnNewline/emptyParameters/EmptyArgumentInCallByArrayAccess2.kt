fun a() {
    val b = listOf(fun(c: Int) {})
    b[0](<caret>)
}

// SET_TRUE: ALIGN_MULTILINE_METHOD_BRACKETS
// IGNORE_FORMATTER