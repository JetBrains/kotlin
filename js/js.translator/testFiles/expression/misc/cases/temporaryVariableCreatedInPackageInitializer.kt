package foo

var p = 0
val c = p++ // creates temporary value

fun box() = (p == 1) && (c == 0)