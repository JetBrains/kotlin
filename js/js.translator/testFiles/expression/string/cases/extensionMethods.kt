package foo

fun box(): Boolean {
    val s = "bar"
    return s.size == 3 && s.length() == 3 && s.length == 3
}