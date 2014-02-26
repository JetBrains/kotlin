package foo

var b = 0

fun loop(times: Int) {
    var left = times
    while (left > 0) {
        val u = {(value: Int) ->
            b = b + 1
        }
        u(left--)
    }
}

fun box(): Boolean {
    loop(5)
    return b == 5
}