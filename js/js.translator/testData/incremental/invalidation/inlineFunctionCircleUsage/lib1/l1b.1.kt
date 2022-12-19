inline fun funB(): Int {
    val f = ::funA
    if (false) {
        return f(false)
    }
    return 1
}
