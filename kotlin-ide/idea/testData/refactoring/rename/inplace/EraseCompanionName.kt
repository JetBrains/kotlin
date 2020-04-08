class Foo {
    companion object <caret>Bar {
        fun bar(n: Int) {}
        operator fun invoke(n: Int) {}
        operator fun get(n: Int) {}
    }
}

fun test() {
    val x = 1
    Foo.Bar.bar(123)
    Foo.Bar(123)
    Foo.Bar[123]
    val y: Foo.Bar
    val z: Foo.Bar?
}