fun <caret>f(p1: Int, p2: Int): Int {
    println(p1)
    println(p2)
    return p1 + p2
}

fun main(args: Array<String>) {
    if (args.size > 0)
        f(1, 2)
    else
        f(3, 4)

    for (i in 1..10)
        f(0, 1)

    when (args.size) {
        0, 1 -> println(f(1, 1))
        else -> println(f(2, 2))
    }

    while (true)
        println(f(5, 6))
}