@Suppress("NOTHING_TO_INLINE")
inline fun <!VIPER_TEXT!>double<!>(x: Int): Int {
    val y = x + x
    return y
}

@Suppress("NOTHING_TO_INLINE")
inline fun <!VIPER_TEXT!>quadruple<!>(x: Int) = double(x) + double(x)

@Suppress("NOTHING_TO_INLINE")
inline fun <!VIPER_TEXT!>branching<!>(b: Boolean): Int {
    if (b) {
        return 1
    } else {
        return 0
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun <!VIPER_TEXT!>use_branching<!>(): Int = branching(false) + branching(true)