fun <caret>f(p1: Int, p2: Int): Int {
    println(p1)
    println(p2)
    return p1 + p2
}

fun foo1() = f(1, 2)

fun foo2(): Int = f(3, 4)

val v: Int
    get() = f(5, 6)
