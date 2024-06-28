fun foo(x: Int): () -> Unit = {
    println(x)
}

fun bar() = 23

// LINES(JS_IR): 1 1 3 3 1 5 5 5 5 1 1 1 2 2 3 3
