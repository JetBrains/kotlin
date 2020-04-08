// SHOULD_FAIL_WITH: Parameter reference can't be safely replaced with this since target function can't be referenced in this context
interface T {
    val foo: Int
}

val f = fun(<caret>t: T) {
    object {
        fun f(): Int {
            return t.foo
        }
    }
}