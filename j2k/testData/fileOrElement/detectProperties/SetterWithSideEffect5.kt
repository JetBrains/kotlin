class C {
    var x = ""
        set(x) {
            println("setter invoked")
            field = x
        }
}