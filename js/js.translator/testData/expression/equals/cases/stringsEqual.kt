package foo

fun box(): Boolean {
    val a = "abc"
    val b = "abc"
    val c = "def"

    if (a != b) return false
    if (a == c) return false

    return true
}