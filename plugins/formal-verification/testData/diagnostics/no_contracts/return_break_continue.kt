fun <!VIPER_TEXT!>test_return<!>(): Int {
    return 0
    return 1
}

fun <!VIPER_TEXT!>return_from_loop<!>(): Int {
    while (true) {
        return 0
    }
    return 1
}