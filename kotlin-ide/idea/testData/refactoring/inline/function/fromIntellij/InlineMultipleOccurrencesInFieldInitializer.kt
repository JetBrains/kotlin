object Foo {

    val bars = arrayOf(Bar.callMe("a", 0, "A", "B", "C"), Bar.callMe("b", 1, "A", "B"))
}


class Bar(a: String, nr: Int, vararg args: String) {
    companion object {

        fun call<caret>Me(a: String, nr: Int, vararg args: String): Bar {
            return Bar(a, nr, *args)
        }
    }
}