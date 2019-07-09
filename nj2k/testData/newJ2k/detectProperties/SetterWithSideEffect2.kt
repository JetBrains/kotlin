class C {
    private var x = ""

    fun getX(): String {
        return x
    }

    fun setX(x: String) {
        println("setter invoked")
        this.x = x
    }

    internal fun foo() {
        x = "a"
    }
}
