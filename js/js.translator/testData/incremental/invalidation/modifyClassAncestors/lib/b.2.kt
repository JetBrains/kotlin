abstract class B : A() {
    override var p2: String = "ello"
    var p3: String = ", "
    abstract var p1: String

    abstract fun f1(): String
    fun f3(): String = "!"
    override fun f2(): String = "orld"
}
