fun foo() {
    // comment 1
    if (true) {

    } <caret>else /* comment 2 */ {
        if (true) {
            // comment 4
            foo()
        }
    }
}