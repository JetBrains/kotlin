
external fun apply_c(arg: Int, x: (Int)-> Int): Int

fun inc(i: Int): Int {
    return i + 1
}

fun dec(i: Int): Int {
    return i - 1
}

fun apply(arg: Int, x: (Int) -> Int): Int {
    return x(arg)
}

fun compact_test(x: Int): Int {
    return apply(apply(apply(x, ::inc), ::inc), ::dec)
}

fun external_test(x: Int): Int {
    return apply_c(apply_c(apply_c(x, ::dec), ::dec), ::inc)
}

fun mixed_test(x: Int): Int {
    return apply(apply_c(apply(apply_c(apply(x, ::inc), ::inc), ::inc), ::dec), ::inc)
}