abstract class B : A() {
    abstract var p1: String
    override var p2: String = "ello"
    var p3: String = ", "

    abstract fun f1(): String
    override fun f2(): String = "orld"
    fun f3(): String = "!"
}
