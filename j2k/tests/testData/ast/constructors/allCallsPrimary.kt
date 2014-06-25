class C(arg1: Int, arg2: Int, arg3: Int) {
    class object {

        fun create(arg1: Int, arg2: Int): C {
            val __ = C(arg1, arg2, 0)
            return __
        }

        fun create(arg1: Int): C {
            val __ = C(arg1, 0, 0)
            return __
        }
    }
}

public class User {
    class object {
        public fun main() {
            val c1 = C(100, 100, 100)
            val c2 = C.create(100, 100)
            val c3 = C.create(100)
        }
    }
}