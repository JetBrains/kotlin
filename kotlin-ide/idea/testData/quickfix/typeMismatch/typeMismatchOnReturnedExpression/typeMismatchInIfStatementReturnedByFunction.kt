// "Change return type of enclosing function 'boo' to 'String'" "true"
fun boo(): Int {
    return ((if (true) {
        val a = ""
        <caret>a
    } else ""))
}