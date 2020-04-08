// "Replace with 'x'" "true"

interface X {
    @Deprecated("", ReplaceWith("x"))
    fun getX(): String

    val x: String
}

fun foo(x: X): String {
    return x.<caret>getX()
}
