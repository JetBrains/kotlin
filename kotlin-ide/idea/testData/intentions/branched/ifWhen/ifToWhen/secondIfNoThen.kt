fun foo(arg: Int) {
    <caret>if (arg == 0) return
    if (arg == 1) else return
}
