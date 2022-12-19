inline fun funA(flag: Boolean = false): Int {
    if (flag) {
        return funB()
    }
    return 2
}
