package foo

fun f(): Int {
    var x: Int = 1
    x = x + 1
    return x
}

fun box() = f() == 2