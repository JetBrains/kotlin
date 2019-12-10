// PROBLEM: none
private fun foo(arg: Any): Int {
    val x =
        if (arg is String)
            (arg.length + 1)
        else
            arg.hashCode()

    if (<caret>x == null) return 0

    return x
}