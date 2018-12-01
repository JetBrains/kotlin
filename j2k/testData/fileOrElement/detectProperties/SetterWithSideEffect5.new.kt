class C {
    var x: String? = ""
        set(x) {
            println("setter invoked")
            field = x
        }
}
