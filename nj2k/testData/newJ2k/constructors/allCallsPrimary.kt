package pack

internal class C @JvmOverloads constructor(arg1: Int, arg2: Int = 0, arg3: Int = 0)

object User {
    fun main() {
        val c1 = C(100, 100, 100)
        val c2 = C(100, 100)
        val c3 = C(100)
    }
}