class C {
    private var myX = ""

    var x: String
        get() = myX
        set(x) {
            println("setter invoked")
            myX = x
        }

    internal fun foo() {
        myX = "a"
    }
}
