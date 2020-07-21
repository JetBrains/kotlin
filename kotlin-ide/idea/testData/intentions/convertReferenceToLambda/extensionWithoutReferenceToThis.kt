// WITH_RUNTIME

class A {
    fun foo(x: String) {
        println(x)
    }

    fun main() {
        "doo".apply(this::<caret>foo)
    }
}