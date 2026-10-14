internal fun doSmth(bar: Int) {}

fun main() {
    val foo = Foo()
    if (foo.bar != null) doSmth(foo.bar)
}