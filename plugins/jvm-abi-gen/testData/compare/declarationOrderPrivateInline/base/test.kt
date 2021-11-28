package test

private inline fun f(): Int {
    return g()
}

private inline fun g() = 1

fun h() = f()
