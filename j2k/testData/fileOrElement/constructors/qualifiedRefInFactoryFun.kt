class C(private val arg1: Int, private val arg2: Int, private val arg3: Int) {

    constructor(arg1: Int, arg2: Int, other: C) : this(arg1, arg2, 0) {
        System.out.println(this.arg1 + other.arg2)
    }
}

class User {
    fun foo() {
        val c1 = C(100, 100, 100)
        val c2 = C(100, 100, c1)
    }
}