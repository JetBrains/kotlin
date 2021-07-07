package test


fun h() = f()

private inline fun g() = 1

private inline fun f(): Int {
    return g()
}
