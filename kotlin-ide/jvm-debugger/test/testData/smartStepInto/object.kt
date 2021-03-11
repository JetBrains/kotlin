fun foo() {
    <caret>val a = object {
        fun f() {
            f2()
        }
    }
}

fun f2() {}