// ERROR: Overload resolution ambiguity:  internal fun C(arg1: kotlin.Int, arg2: kotlin.Int): C defined in root package public constructor C(arg1: kotlin.Int, arg2: kotlin.Int = ..., arg3: kotlin.Int = ...) defined in C
fun C(arg1: Int, arg2: Int): C {
    var arg2 = arg2
    val __ = C(arg1, arg2, 0)
    arg2++
    return __
}

class C(arg1: Int, arg2: Int = 0, arg3: Int = 0) {
    private val field: Int

    {
        var arg1 = arg1
        var arg3 = arg3
        arg1++
        System.out.print(arg1 + arg2)
        field = arg3
        arg3++
    }
}

public class User {
    default object {
        public fun main() {
            val c1 = C(100, 100, 100)
            val c2 = C(100, 100)
            val c3 = C(100)
        }
    }
}