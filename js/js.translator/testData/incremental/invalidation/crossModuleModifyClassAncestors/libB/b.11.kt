interface B {
    val i: Int
        get() = 42
    val p1: String
    val p2: String
        get() = "ello"
    val p3: String
        get() = ", "

    fun f(): Int = 42
    abstract fun f1(): String
    open fun f2(): String = "orld"
    fun f3(): String = "!"
}
