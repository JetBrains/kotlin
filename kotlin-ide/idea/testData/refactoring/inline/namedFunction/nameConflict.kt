fun onc<caret>e(p: Int): Int {
    val v = p + 1
    return v
}

fun callShadow() {
    val v = once(1)
}