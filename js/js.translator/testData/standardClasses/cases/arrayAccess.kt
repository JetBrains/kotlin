package foo

fun box(): Boolean {
    val a = arrayOfNulls<Int>(4)
    a[1] = 2
    a[2] = 3
    return (a[1] == 2) && (a[2] == 3)
}