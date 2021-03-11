fun foo() {
    <caret>if (true) if (false) {
        foo()
    }
}