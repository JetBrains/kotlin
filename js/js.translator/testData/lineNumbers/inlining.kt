fun box() {
    println("1")
    bar()
    println("2")
    bar()
    println("3")
}

inline fun bar() {
    println("bar1")
    println("bar2")
}

// LINES: 2 10 11 4 10 11 6 10 11
