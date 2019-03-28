class C {
    var x = ""
        internal
        set(x) {
            println("setter invoked")
            if (other != null) {
                other!!.x = x
            }
            field = x
        }
    internal var other: C? = null
}