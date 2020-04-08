fun foo(arg: Int): Int {
    // 1
    <caret>if (arg == 0) return 0
    // 2
    else return -1
}