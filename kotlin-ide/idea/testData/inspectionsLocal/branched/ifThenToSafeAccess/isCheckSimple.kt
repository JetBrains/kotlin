// HIGHLIGHT: INFORMATION
class My(val x: Int)

fun foo(arg: Any?): My? {
    return <caret>if (arg is My) arg else null
}