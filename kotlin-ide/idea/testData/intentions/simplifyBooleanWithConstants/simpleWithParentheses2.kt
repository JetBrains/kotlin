fun foo(y: Boolean) {
    y || <caret>(y && true)
}