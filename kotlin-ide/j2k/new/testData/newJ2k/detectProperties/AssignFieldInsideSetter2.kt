class C {
    private var x = ""
    var other: C? = null
    fun getX(): String {
        return x
    }

    fun setX(x: String) {
        println("setter invoked")
        if (other != null) {
            other!!.x = x
        }
        this.x = x
    }
}