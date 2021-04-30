// !IGNORE_FIR

fun foo(bar: Any) = when(bar) {
    is String -> bar
    !is String -> "<error>"
}
