fun foo(i: Int) {
    <caret>Integer.valueOf(1)
}

// EXPECTED: null