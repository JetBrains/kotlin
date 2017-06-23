fun foo(x: Int): () -> Unit = {
    println(x)
}

fun bar() = 23

// LINES: 1 1 1 2 2 1 1 1 5 5 5