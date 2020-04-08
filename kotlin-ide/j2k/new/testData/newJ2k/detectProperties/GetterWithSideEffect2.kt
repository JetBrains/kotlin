class C {
    private var x = ""
    fun getX(): String {
        println("getter invoked")
        return x
    }

    fun setX(x: String) {
        this.x = x
    }

    fun foo() {
        println("x = $x")
    }
}