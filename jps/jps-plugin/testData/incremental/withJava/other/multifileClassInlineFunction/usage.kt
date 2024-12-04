package test

fun main(args: Array<String>) {
    f { println("to be inlined") }
    other.f { println("to be inlined") }
}
