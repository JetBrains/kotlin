fun compound(): String {
    val s = "Hello"
    return s
}

fun outer() {
    class Container {
        val v = <caret>compound()
    }
}