fun foo(): IntRange = 2..<6

fun bar(): ClosedRange<String> {
    return "alpha" .. "omega"
}

fun baz(): OpenEndRange<String> {
    return "alpha" ..< "omega"
}
