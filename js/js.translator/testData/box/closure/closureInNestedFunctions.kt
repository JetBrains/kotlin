// EXPECTED_REACHABLE_NODES: 500
package foo

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

    return myRun {
        myRun { result }
    }
}

fun funlit(): Boolean {
    val result = true

    fun foo(): Boolean {
        return myRun { result }
    }

    return foo()
}

fun litfun(): Boolean {
    val result = true

    return myRun {
        fun bar() = result
        bar()
    }
}

fun box(): String {
    if (!funfun()) return "funfun failed"
    if (!litlit()) return "litlit failed"
    if (!funlit()) return "funlit failed"
    if (!litfun()) return "litfun failed"

    return "OK"
}
