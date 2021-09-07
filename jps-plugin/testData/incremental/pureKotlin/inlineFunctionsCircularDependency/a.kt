package test

inline fun a(body: () -> Unit) {
    println("i'm inline function a")
    body()
}

fun main(args: Array<String>) {
    b { println("to be inlined") }
}
