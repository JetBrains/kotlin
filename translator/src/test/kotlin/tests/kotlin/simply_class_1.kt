class classname_A(val x: Int) {
    fun method(): Int {
        return this.x + 5
    }
}

fun simply_class_1(zz: Int): Int {
    val x = classname_A(zz)
    return x.method()
}