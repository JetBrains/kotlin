internal interface I {
    fun getX(): Int
    fun setX(x: Int)
}

internal class A(private var x: Int) : I {

    override fun getX(): Int {
        return x
    }

    override fun setX(x: Int) {
        this.x = x
    }
}
