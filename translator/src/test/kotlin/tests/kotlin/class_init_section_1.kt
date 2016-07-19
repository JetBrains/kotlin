class className(var z: Int) {
    init {
        this.z = this.z + 12345
    }

    fun getVal(): Int {
        return this.z
    }
}

fun class_init_section_1(x: Int): Int {
    val z = className(x)
    return z.getVal()
}