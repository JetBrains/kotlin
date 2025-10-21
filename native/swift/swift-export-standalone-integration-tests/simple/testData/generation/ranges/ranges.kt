fun foo(): IntRange = 2..<6

fun bar(): ClosedRange<Double> {
    return 2.71 .. 3.14
}

fun baz(): OpenEndRange<String> {
    return "alpha" ..< "omega"
}
