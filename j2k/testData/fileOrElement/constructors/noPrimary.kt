fun C(arg1: Int, arg2: Int, arg3: Int): C {
    return C()
}

fun C(arg1: Int, arg2: Int): C {
    val __ = C(arg1, arg2, 0)
    System.out.println()
    return __
}

fun C(arg: Int): C {
    val __ = C()
    System.out.println(arg)
    return __
}

class C

public class User {
    default object {
        public fun main() {
            val c1 = C(1, 2, 3)
            val c2 = C(5, 6)
            val c3 = C(7)
        }
    }
}