class C {
    var x: String? = ""
        set(x) {
            println("old value: " + this.x)
            field = x
        }
}
