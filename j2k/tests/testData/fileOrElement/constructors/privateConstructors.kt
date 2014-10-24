// ERROR: Too many arguments for internal fun C(arg1: kotlin.Int): C defined in root package
// ERROR: Too many arguments for internal fun C(arg1: kotlin.Int): C defined in root package
fun C(arg1: Int): C {
    return C(arg1, 0, 0)
}

class C private(arg1: Int, arg2: Int, arg3: Int = 0)
