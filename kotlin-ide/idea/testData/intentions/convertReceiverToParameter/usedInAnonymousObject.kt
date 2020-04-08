interface A

fun <caret>A.foo() {}

fun test() {
    object : A {
        fun bar() {
            foo()
        }
    }
}