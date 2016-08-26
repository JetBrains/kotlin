class get_set_operators_1_class {
    var cap = 9

    operator fun get(x: Int): Int {
        return x + 8
    }

    operator fun set(x: Int, y: Int): Unit {
        cap = x + y
    }
}

fun get_set_operators_1_get(arg: Int): Int {
    val b = get_set_operators_1_class()
    return b[arg]
}

fun get_set_operators_1_set(ind: Int, arg: Int): Int {
    val b = get_set_operators_1_class()
    b[ind] = arg
    return b.cap
}