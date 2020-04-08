package testing

object Object {
    fun foo() {
        "".foo()
    }

    fun foo(a: Int) {
    }

    fun String.foo() {
    }

}

fun main(args: Array<String>) {
    Object.foo()
    Object.foo(1)
}