package q

external class Foo {
    // RUN:
    fun foo(s: Array<String>)
}


// RUN:
external fun main(s: Array<String>) {
    println("Top-level")
}