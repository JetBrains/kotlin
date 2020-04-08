package test

object Object {
    fun f() {
    }

    fun Int.f() {
    }

    private fun privateFun() {
    }

    val Int.g: Int
        get() = this + 2

    fun <T, K, G> complexFun(a: T, b: K, c: G): G {
        throw RuntimeException()
    }
}
