class C {
    var x: String? = ""
        set(value) {
            println("setter invoked")
            field = value
        }
}