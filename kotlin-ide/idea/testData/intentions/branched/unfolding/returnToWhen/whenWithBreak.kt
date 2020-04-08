fun test(b: Boolean): Int {
    loop@ while (true) {
        <caret>return when (b) {
            true -> 1
            else -> break@loop
        }
    }
    return 0
}