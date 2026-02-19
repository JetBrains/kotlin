abstract class B : A() {
    abstract var p1: String
    override var p2: String = "ELLO"
    var p3: String = ", "

    abstract fun f1(): String
    override fun f2(): String = "ORLD"
    fun f3(): String = "!"
}
