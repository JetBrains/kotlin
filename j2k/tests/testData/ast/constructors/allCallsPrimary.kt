package pack


fun C(arg1: Int, arg2: Int): C {
    return C(arg1, arg2, 0)
}

fun C(arg1: Int): C {
    return C(arg1, 0, 0)
}

class C(arg1: Int, arg2: Int, arg3: Int)

public class User {
    class object {
        public fun main() {
            val c1 = C(100, 100, 100)
            val c2 = C(100, 100)
            val c3 = C(100)
        }
    }
}