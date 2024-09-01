package foo

fun box(): String {
    return if (!false) "OK" else "fail"
}