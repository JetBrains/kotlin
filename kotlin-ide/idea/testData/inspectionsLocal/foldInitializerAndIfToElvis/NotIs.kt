fun foo(arg: Any?) {
    val n = arg
    if (<caret>n !is Int) return
}