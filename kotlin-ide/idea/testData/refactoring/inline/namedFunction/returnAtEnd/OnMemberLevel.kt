fun compound(): String {
    val s = "Hello"
    return s
}

class Container {
    val v = <caret>compound()
}