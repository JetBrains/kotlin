package foo

class A() {
    fun lold() = true

    val p = {
        {
            lold()
        }()
    }
}

fun box(): Boolean {
    return A().p()
}
