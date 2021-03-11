class C {
    private var x = ""
    internal var other: C? = null

    fun getX(): String {
        return x
    }

    internal fun setX(x: String) {
        println("setter invoked")
        if (other != null) {
            this.other!!.x = x
        }
        this.x = x
    }
}
