class class_methods_1_class() {
    fun method(t : Int): Int {
        return t + 5
    }
}

fun class_methods_1(zz: Int): Int {
    val x = class_methods_1_class()
    return x.method(zz)
}