package test

fun bump(): Int {
    Counter.n += 1
    return Counter.n
}
