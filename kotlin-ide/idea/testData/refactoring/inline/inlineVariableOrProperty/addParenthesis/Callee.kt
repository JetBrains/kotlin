fun foo(x : String, y : () -> String.() -> Unit) {
    val <caret>f = y()
    x.f()
}