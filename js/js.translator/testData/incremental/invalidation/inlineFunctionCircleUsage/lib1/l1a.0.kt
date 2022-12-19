inline fun funA(flag: Boolean = true): Int {
    if (flag) {
        return funB()
    }
    return 2
}
