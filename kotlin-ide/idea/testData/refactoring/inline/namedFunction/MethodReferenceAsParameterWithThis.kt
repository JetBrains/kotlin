class A {
    fun foo(x: String) {
        println(x)
    }

    fun main() {
        "foo".let(this::<caret>foo)

        "doo".apply(::foo)
    }
}