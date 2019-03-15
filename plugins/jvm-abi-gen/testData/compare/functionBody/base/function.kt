package test

fun sum(x: Int, y: Int): Int =
    try {
        var result = x
        result += y
        result
    } finally {
        // do nothing
    }