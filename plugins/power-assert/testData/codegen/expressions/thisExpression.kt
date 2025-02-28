fun box(): String {
    with(A()){
        return test1() +
                test2() +
                false.test3() +
                1.test4() +
                1.test5() +
                true.test6() +
                1.test7()
    }
}

class A {
    val a: Boolean = false
    val b: Int = 1

    fun test1() = expectThrowableMessage {
        assert(this.a)
    }

    fun test2() = expectThrowableMessage {
        assert(this.b == 2)
    }

    fun Boolean.test3() = expectThrowableMessage {
        assert(this)
    }

    fun Int.test4() = expectThrowableMessage {
        assert(this == 2)
    }

    val test5 = fun Int.() = expectThrowableMessage {
        assert(this == 2)
    }

    fun Boolean.test6() = expectThrowableMessage {
        assert(this@A.a)
    }

    val test7 = lambda@ fun Int.() = expectThrowableMessage {
        assert(this@lambda == 2)
    }

    override fun toString(): String {
        return "A"
    }
}