infix fun String.xxx(p: Int): String = this

fun foo(x: String?) {
    x!!.<caret>xxx(1)
}
