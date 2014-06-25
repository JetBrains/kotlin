class C(private val arg1: Int, private val arg2: Int, private val arg3: Int) {

    fun foo(p: Int): Int {
        return p
    }

    class object {
        private fun staticFoo(p: Int): Int {
            return p
        }

        fun create(arg1: Int, arg2: Int, other: C): C {
            val __ = C(arg1, arg2, 0)
            System.out.println(__.foo(1) + __.foo(2) + other.foo(3) + staticFoo(4) + C.staticFoo(5))
            return __
        }
    }
}