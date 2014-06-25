class C(val myArg1: Int) {
    var myArg2: Int = 0
    var myArg3: Int = 0

    {
        myArg2 = 0
        myArg3 = 0
    }

    class object {

        fun create(arg1: Int, arg2: Int, arg3: Int): C {
            val __ = C(arg1)
            __.myArg2 = arg2
            __.myArg3 = arg3
            return __
        }

        fun create(arg1: Int, arg2: Int): C {
            val __ = C(arg1)
            __.myArg2 = arg2
            __.myArg3 = 0
            return __
        }
    }
}

public class User {
    class object {
        public fun main() {
            val c1 = C.create(100, 100, 100)
            val c2 = C.create(100, 100)
            val c3 = C(100)
        }
    }
}