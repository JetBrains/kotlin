internal class A {
    fun acceptDouble(d: Double) {}
    fun acceptDoubleBoxed(d: Double?) {}
    fun conversion() {
        val a = 10
        val b = 0.7f
        acceptDouble((a * b).toDouble())
        acceptDoubleBoxed((a * b).toDouble())
    }
}
