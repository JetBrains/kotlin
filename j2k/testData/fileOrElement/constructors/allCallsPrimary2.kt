internal class C internal constructor(internal val myArg1: Int) {
    internal var myArg2: Int = 0
    internal var myArg3: Int = 0

    internal constructor(arg1: Int, arg2: Int, arg3: Int) : this(arg1) {
        myArg2 = arg2
        myArg3 = arg3
    }

    internal constructor(arg1: Int, arg2: Int) : this(arg1) {
        myArg2 = arg2
        myArg3 = 0
    }

    init {
        myArg2 = 0
        myArg3 = 0
    }
}

object User {
    fun main() {
        val c1 = C(100, 100, 100)
        val c2 = C(100, 100)
        val c3 = C(100)
    }
}