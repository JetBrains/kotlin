class C {
    var x: String? = ""
        get() {
            println("getter invoked")
            return field
        }
}
