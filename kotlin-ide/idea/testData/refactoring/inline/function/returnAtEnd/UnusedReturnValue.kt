fun <caret>f(p: Int): Boolean {
    println(p)
    return p > 0
}

fun g(): Int = TODO()

fun main(args: Array<String>) {
    f(1)
    f(g())
}