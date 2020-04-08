fun <caret>f(p1: Int, p2: Int): Int {
    println(p1)
    println(p2)
    return p1 + p2
}

fun main(args: Array<String>) {
    println(f(3, 5))
}