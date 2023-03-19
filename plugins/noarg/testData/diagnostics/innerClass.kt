annotation class NoArg

class Outer {
    @NoArg
    inner class <!NOARG_ON_INNER_CLASS!>Inner<!>(val b: Any)
}

fun local() {
    @NoArg
    class <!NOARG_ON_LOCAL_CLASS!>Local<!>(val l: Any) {
        @NoArg
        inner class <!NOARG_ON_INNER_CLASS!>InnerLocal<!>(val x: Any)
    }
}
