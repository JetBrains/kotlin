fun foo(): Int {
    val a = 1
    val t = <caret>if (a > 1) a else return 0

    return t
}