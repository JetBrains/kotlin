class My(val x: Int)

fun foo(arg: Any?): My {
    return if (<caret>arg !is My) My(42) else arg
}