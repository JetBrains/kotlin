package foo

val a = arrayOfNulls<Int>(3)

fun box() = (a[0] == null && a[1] == null && a[2] == null)