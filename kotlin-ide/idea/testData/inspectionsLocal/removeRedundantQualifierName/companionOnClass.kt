package my.simple.name
import my.simple.name.Foo.Companion.VARIABLE

class Foo {
    companion object {
        const val VARIABLE = 1
    }
}

fun main() {
    val a = my.simple.name.Foo.VARIABLE
    val b = my.simple.name.Foo.Companion.VARIABLE
    val c = my.simple.name<caret>.Foo()
}