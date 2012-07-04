package foo

fun box(): Boolean {
    val a = "abc"
    val b = "def"
    val message = "a = $a, b = $b"

    if (message != "a = abc, b = def") return false

    return true
}