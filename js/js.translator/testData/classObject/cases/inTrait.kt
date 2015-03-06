package foo

trait A {
    default object {
        val OK: String = "OK"
    }
}

fun box(): String {
    return A.OK
}