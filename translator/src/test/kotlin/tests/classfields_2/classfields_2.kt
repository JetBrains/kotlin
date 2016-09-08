class classfields_2_class {
    var i = 0

    fun method() {
        i = 5
    }

    fun changeI(): Int {
        method()
        return i
    }
}

fun test1(): Int {
    val main = classfields_2_class()
    return main.changeI()
}

fun test2(): Int {
    val main = classfields_2_class()
    return main.i
}

fun test3(): Int {
    return classfields_2_class().changeI()
}
