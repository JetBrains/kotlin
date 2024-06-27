interface B {
    val p1: String
    val p2: String
        get() = "ELLO"
    val p3: String
        get() = ", "

    abstract fun f1(): String
    open fun f2(): String = "ORLD"
    fun f3(): String = "!"
}
