fun do_while_test_1(x: Int): Int {
    var t = x
    do {
        t = t + 1
    } while (2 > 3)
    return t
}