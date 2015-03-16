class C(arg1: Int, arg2: Int, arg3: Int) {

    constructor(arg1: Int, arg2: Int) : this(arg1, arg2, 0) {
        System.out.println()
    }

    constructor(arg1: Int) : this(arg1, 0) {
        System.out.println()
    }
}

public class User {
    default object {
        public fun main() {
            val c1 = C(1, 2, 3)
            val c2 = C(5, 6)
            val c3 = C(7)
        }
    }
}