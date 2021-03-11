fun test() {
    with(foo()) {
        println("test")
    }
}

fun foo(): String {
    println("foo")
    return ""
}
