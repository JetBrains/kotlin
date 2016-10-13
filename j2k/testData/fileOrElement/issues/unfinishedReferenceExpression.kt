// ERROR: Expression 'this' cannot be a selector (occur after a dot)
class JavaClass {
    var v = ""
    fun m(s: String) {
        s.
                this.v.
    }
}