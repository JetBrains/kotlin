fun test(n: Int): String {
    var s = "test"
    <caret>when (n) {
        0 -> s = "zero"
        1 -> s = "one"
        2 -> s = "two"
    }
    return s
}
