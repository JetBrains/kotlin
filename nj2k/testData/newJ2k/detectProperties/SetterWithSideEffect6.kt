class C {
    var x = ""
        set(x) {
            println("old value: " + this.x)
            field = x
        }
}
