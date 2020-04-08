fun bar(arg: Int): Int {
    <caret>if (arg < 0) {
        if (arg == -3) return 0
    }
    if (arg > 0) {
        return 1
    }
    return -1
}