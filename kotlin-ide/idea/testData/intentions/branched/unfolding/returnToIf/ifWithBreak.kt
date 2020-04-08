fun test(b: Boolean): Int {
    while (true) {
        <caret>return if (b) {
            1
        } else {
            break
        }
    }
    return 0
}