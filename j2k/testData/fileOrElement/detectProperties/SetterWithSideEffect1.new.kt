class C {
    private var myX: String? = ""

    var x: String?
        get() = myX
        set(x) {
            println("setter invoked")
            myX = x
        }

    internal fun foo() {
        myX = "a"
    }
}
