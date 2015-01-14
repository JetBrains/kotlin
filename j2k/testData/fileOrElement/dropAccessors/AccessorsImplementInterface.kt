trait I {
    public fun getX(): Int
    public fun setX(x: Int)
}

class A(private var x: Int) : I {

    override fun getX(): Int {
        return x
    }

    override fun setX(x: Int) {
        this.x = x
    }
}
