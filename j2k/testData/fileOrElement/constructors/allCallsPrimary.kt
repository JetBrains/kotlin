package pack

class C jvmOverloads constructor(arg1: Int, arg2: Int = 0, arg3: Int = 0)

public object User {
    public fun main() {
        val c1 = C(100, 100, 100)
        val c2 = C(100, 100)
        val c3 = C(100)
    }
}