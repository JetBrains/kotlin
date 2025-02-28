fun box(): String {
    with(A()) {
        return runAll(
            "test1" to { test1() },
            "test2" to { test2() },
            "test3" to { false.test3() },
            "test4" to { 1.test4() },
            "test5" to { 1.test5() },
            "test6" to { true.test6() },
            "test7" to { 1.test7() },
        )
    }
}

class A {
    val a: Boolean = false
    val b: Int = 1

    fun test1() {
        assert(this.a)
    }

    fun test2() {
        assert(this.b == 2)
    }

    fun Boolean.test3() {
        assert(this)
    }

    fun Int.test4() {
        assert(this == 2)
    }

    val test5 = fun Int.() {
        assert(this == 2)
    }

    fun Boolean.test6() {
        assert(this@A.a)
    }

    val test7 = lambda@ fun Int.() {
        assert(this@lambda == 2)
    }

    override fun toString(): String {
        return "A"
    }
}