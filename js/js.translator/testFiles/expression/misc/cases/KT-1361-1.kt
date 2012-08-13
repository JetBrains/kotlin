package foo

class B {
    val d = true

    fun f(): Boolean {
        val c = object {
            fun foo(): Boolean {
                return d
            }
            fun boo(): Boolean {
                return foo()
            }
        }
        return c.boo()
    }
}

fun box(): Boolean {
    return B().f()
}