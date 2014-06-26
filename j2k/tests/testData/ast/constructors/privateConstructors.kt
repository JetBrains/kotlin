private fun C(arg1: Int, arg2: Int): C {
    return C(arg1, arg2, 0)
}

public fun C(arg1: Int): C {
    return C(arg1, 0, 0)
}

class C private(arg1: Int, arg2: Int, arg3: Int)
