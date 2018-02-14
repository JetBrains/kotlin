enum class E(val x: Int = 0) {
    A,
    B,
    C(1) {
        override fun enumFun() = 42
    };

    open fun enumFun(): Int = 0
    val enumVal = 0
    var enumVar = ""
}