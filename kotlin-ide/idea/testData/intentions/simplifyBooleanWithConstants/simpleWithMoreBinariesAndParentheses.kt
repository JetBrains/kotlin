fun foo(y: Boolean) {
    false || false || y || y || <caret>false && (y && y || true)
}