fun foo(): Int {
    <caret>println("")
    return 10
}

// OUT_OF_BLOCK: false