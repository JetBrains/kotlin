// IGNORE_BACKEND_K1: ANY
fun box(): String {
    with(A()) {
        return runAll(
            "test1" to { test1() },
            "test2" to { test2() },
            "test3" to { test3() },
        )
    }
}

open class Base {
    val a: Boolean = false
    val b: Int = 1
    fun foo(): Boolean = false
}

class A : Base() {
    fun test1() {
        assert(super.a)
    }

    fun test2() {
        assert(super.b == 2)
    }

    fun test3() {
        assert(super<Base>.foo())
    }
}