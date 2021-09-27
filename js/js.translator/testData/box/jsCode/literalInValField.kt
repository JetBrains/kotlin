val p1 = "O"
val p2 = "K"
val pp = p1 + p2

fun bar(): String {
    val v = pp
    val b = js("\"$v\"")
    return b
}

fun box(): String = bar()