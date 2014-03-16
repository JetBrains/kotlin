package foo

trait A {
    class object {
        val OK: String = "OK"
    }
}

fun box(): String {
    return A.OK
}