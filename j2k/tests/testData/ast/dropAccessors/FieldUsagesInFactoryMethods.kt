fun C(arg1: Int, arg2: Int, arg3: Int): C {
    val __ = C(arg1)
    __.arg2 = arg2
    __.arg3 = arg3
    return __
}

fun C(arg1: Int, arg2: Int): C {
    val __ = C(arg1)
    __.arg2 = arg2
    __.arg3 = 0
    return __
}

class C(val arg1: Int) {
    var arg2: Int = 0
    var arg3: Int = 0

    {
        arg2 = 0
        arg3 = 0
    }
}