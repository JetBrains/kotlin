class Test {
    var a: Boolean
        get() = false
        set(
        <caret>
           )
}

// SET_TRUE: ALIGN_MULTILINE_METHOD_BRACKETS
// IGNORE_FORMATTER