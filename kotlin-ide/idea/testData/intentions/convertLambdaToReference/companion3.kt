class C {
    companion object {
        fun foo(s: String) = 1
    }
}

class Test {
    val f = {<caret> s: String -> C.foo(s) }
}