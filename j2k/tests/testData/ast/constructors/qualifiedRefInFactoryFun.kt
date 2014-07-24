fun C(arg1: Int, arg2: Int, other: C): C {
    val __ = C(arg1, arg2, 0)
    System.out.println(__.arg1 + other.arg2)
    return __
}

class C(private val arg1: Int, private val arg2: Int, private val arg3: Int)

class User {
    fun foo() {
        val c1 = C(100, 100, 100)
        val c2 = C(100, 100, c1)
    }
}