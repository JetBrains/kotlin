fun box() {
    println("1")
    foo {
        println("x")
    }
    println("2")
    foo {
        println("y")
    }
    println("3")
}

inline fun foo(f: () -> Unit) {
    println("before")
    f()
    println("after")
}

// LINES: 2 14 4 16 6 14 8 16 10 14 15 16