fun a() {
    println(1 + null
        <caret>?: 2)
}

// SET_FALSE: CONTINUATION_INDENT_IN_ELVIS
// WITHOUT_CUSTOM_LINE_INDENT_PROVIDER