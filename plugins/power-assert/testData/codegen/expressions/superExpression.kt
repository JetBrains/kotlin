// IGNORE_BACKEND_K1: ANY

fun box(): String {
    with(A()){
        return test1() +
                test2() +
                test3()
    }
}
open class Base {
    val a: Boolean = false
    val b: Int = 1
    fun foo(): Boolean = false
}

class A : Base() {
    fun test1() = expectThrowableMessage {
        assert(super.a)
    }

    fun test2() = expectThrowableMessage {
        assert(super.b == 2)
    }

    fun test3() = expectThrowableMessage {
        assert(super<Base>.foo())
    }
}