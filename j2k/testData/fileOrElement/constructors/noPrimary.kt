internal open class Base

internal class C : Base {
    constructor(arg1: Int, arg2: Int, arg3: Int) {}

    constructor(arg1: Int, arg2: Int) : this(arg1, arg2, 0) {
        println()
    }

    constructor(arg: Int) {
        println(arg)
    }
}

object User {
    fun main() {
        val c1 = C(1, 2, 3)
        val c2 = C(5, 6)
        val c3 = C(7)
    }
}