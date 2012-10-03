package foo

fun box(): Boolean {
    val a = "abc"
    val b = "def"
    val message = "a = $a, b = $b"

    if (message != "a = abc, b = def") return false

    val v1 = null
    if ("returns null null" != "returns $v1 ${null}") return false

    return true
}