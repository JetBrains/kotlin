fun foo(x: Int): () -> Unit = {
    println(x)
}

fun bar() = 23

// LINES(JS):    1 1 1 3 2 2 3 3 1 1 1 5 5 5
// LINES(JS_IR):             3 3 1 *     5 5 * 2 2
