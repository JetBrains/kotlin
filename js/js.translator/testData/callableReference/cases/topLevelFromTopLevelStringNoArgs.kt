package foo

fun bar() = "OK"

fun box(): String {
    val x = ::bar
    return x()
}
