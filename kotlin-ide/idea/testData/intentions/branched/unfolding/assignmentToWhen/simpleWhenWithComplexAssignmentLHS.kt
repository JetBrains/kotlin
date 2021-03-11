fun test(n: Int): Array<String> {
    var x: Array<String> = arrayOfNulls<String>(1) as Array<String>

    x[0] = <caret>when(n) {
        in 0..10 -> "small"
        in 10..100 -> "average"
        in 100..1000 -> "big"
        else -> "unknown"
    }

    return x
}
