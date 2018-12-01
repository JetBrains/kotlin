class C {
    private var myX: String? = ""

    var x: String?
        get() {
            println("getter invoked")
            return myX
        }
        set(x) {
            myX = x
        }

    internal fun foo() {
        println("myX = $myX")
    }
}