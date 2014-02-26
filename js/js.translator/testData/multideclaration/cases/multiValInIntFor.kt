package foo

fun Int.component1(): Int {
    return this + 10;
}

fun Int.component2(): String = "b"

fun box(): String {
    var i = 0;
    var s = "";
    for ((a, b) in 1..4) {
        i = a
        s = b
    }

    if (i != 14) return "i != 14, it: $i"
    if (s != "b") return "s != 'b', it: $s"

    return "OK"
}