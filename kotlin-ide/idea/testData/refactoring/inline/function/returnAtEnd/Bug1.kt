fun <caret>f(p1: Int, p2: Int): Int {
    println(p1)
    println(p2)
    return p1 + p2
}

fun main(args: Array<String>) {
    for (i in 1..10) {
        println(f(i, i + 1))
    }
}