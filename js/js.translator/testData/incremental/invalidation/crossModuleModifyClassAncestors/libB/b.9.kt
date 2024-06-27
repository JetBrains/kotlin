interface B {
    val p2: String
        get() = "ello"
    val p1: String
    val p3: String
        get() = ", "

    open fun f2(): String = "orld"
    abstract fun f1(): String
    fun f3(): String = "!"
}
