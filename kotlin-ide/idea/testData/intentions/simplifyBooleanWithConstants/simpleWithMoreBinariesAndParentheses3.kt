fun foo(y: Boolean) {
    ((y && true) || false) <caret>&& (true && (y && (y && (y ||false))))
}