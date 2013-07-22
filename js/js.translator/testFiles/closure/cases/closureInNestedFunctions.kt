package foo

fun run<T>(f: ()->T) = f()

fun funfun(): Boolean {
    val result = true

    fun foo(): Boolean {
        fun bar() = result
        return bar()
    }

    return foo()
}

fun litlit(): Boolean {
    val result = true

    return run {
        run { result }
    }
}

fun funlit(): Boolean {
    val result = true

    fun foo(): Boolean {
        return run { result }
    }

    return foo()
}

fun litfun(): Boolean {
    val result = true

    return run {
        fun bar() = result
        bar()
    }
}

fun box(): String {
    if (!funfun()) "funfun failed"
    if (!litlit()) "litlit failed"
    if (!funlit()) "funlit failed"
    if (!litfun()) "litfun failed"

    return "OK"
}