fun foo(): String {
    var x = 0
    do {
        x++
        var y = x + 5
    } while (y < 10)
    if (x != 5) "Fail"
    return "O"
}

fun bar(): String {
    var b = false
    do {
        var x = "X"
        var y = "Y"
        b = true
    } while (x + y != "XY")
    if (!b) return "ZZZ"
    return "K"
}

fun box(): String {
    return foo() + bar()
}
