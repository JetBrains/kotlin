class AAA {
    private val x = 42
    private val other = AAA()

    fun getX(): Int {
        return other.x
    }

    fun issue(): Boolean {
        return true
    }
}
