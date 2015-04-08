package foo

val f = { i: Int -> i + 1 }

val a = Array(3, f)

fun box() = (a[0] == 1 && a[2] == 3 && a[1] == 2)
