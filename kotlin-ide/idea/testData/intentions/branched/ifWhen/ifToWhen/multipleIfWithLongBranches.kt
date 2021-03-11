fun foo(arg: Int): Int {
    <caret>if (arg < 0) {
        var x = arg + 1
        x++
        return x
    }
    if (arg > 0) {
        var y = arg - 1
        y--
        return y
    }
    var z = arg
    z *= 2
    return z
}
