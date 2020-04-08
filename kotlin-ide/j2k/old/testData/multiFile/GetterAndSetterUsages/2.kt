class B {

    var yy = ""
        private set

    internal fun foo(a: AAA) {
        a.x = a.x + 1
        yy += "a"
    }
}
