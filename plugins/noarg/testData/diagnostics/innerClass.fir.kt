annotation class NoArg

class Outer {
    @NoArg
    inner class <!NOARG_ON_INNER_CLASS_ERROR!>Inner<!>(val b: Any)
}

fun local() {
    @NoArg
    class Local(val l: Any) {
        @NoArg
        inner class InnerLocal(val x: Any)
    }
}
