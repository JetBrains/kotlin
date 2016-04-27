class C {
    protected var x = ""

    fun getX(): String {
        return x
    }

    fun setX(x: String) {
        println("setter invoked")
        this.x = x
    }
}
