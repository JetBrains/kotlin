internal class C(private val arg1: Int, private val arg2: Int, private val arg3: Int) {

    fun foo(p: Int): Int {
        return p
    }

    constructor(arg1: Int, arg2: Int, other: C) : this(arg1, arg2, 0) {
        println(foo(1) + this.foo(2) + other.foo(3) + staticFoo(4) + C.staticFoo(5))
    }

    companion object {
        private fun staticFoo(p: Int): Int {
            return p
        }

        fun staticFoo2(): Int {
            return 0
        }
    }
}
