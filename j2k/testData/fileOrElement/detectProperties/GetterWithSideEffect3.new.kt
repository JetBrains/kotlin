class C {
    var x: String? = ""

    fun getX(): String? {
        println("getter invoked")
        return x
    }

    fun setX(x: String?) {
        this.x = x
    }
}
