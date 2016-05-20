class C {
    private var myX = ""

    var x: String
        get() = myX
        set(x) {
            println("setter invoked")
            this.myX = x
        }

    internal fun foo() {
        myX = "a"
    }
}
