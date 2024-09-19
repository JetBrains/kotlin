fun foo(p: String) {
    val i: String = "i"
    foo(i)
    println("test")
}

fun main() {
    foo("main")
}