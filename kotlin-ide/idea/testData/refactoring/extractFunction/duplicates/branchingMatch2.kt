// WITH_RUNTIME
// PARAM_TYPES: kotlin.Int
// PARAM_TYPES: kotlin.Int
// PARAM_DESCRIPTOR: value-parameter a: kotlin.Int defined in test
// PARAM_DESCRIPTOR: var b: kotlin.Int defined in test

// SIBLING:
fun test(a: Int): Int {
    var b: Int = 1
    <selection>if (a > 0) {
        b = b + a
    }
    else {
        b = b - a
    }</selection>
    return b
}

fun foo1() {
    val x = 1
    var y: Int = x
    println(
            if (x > 0) {
                y + x
            }
            else {
                y - x
            }
    )
}

fun foo2(x: Int) {
    var p: Int = 1
    if (x > 0) {
        p = p + x
    }
    else {
        p = p - x
    }
    println(p)
}

fun foo3(x: Int): Int {
    var p: Int = 1
    if (x > 0) {
        return p + x
    }
    else {
        return p - x
    }
}

fun foo4() {
    val t: (Int) -> (Int) = {
        var n = it
        if (it > 0) {
            n + it
        }
        else {
            n - it
        }
    }
    println(t(1))
}

fun foo5(x: Int): Int {
    var p: Int = 1
    if (x > 0) {
        val t = p + x
    }
    else {
        val u = p - x
    }
}
