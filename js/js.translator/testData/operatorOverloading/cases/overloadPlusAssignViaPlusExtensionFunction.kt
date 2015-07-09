package foo

open class Foo<out T>(open val value: T)
open class MutableFoo<T>(override var value: T): Foo<T>(value)

fun <T> Foo<T>.plus(x: T): Foo<T> = Foo(x)

// overloading:
fun <T> MutableFoo<T>.plus(x: T): MutableFoo<T> = MutableFoo(x)


fun box(): Boolean {
    var f = MutableFoo(1)
    f += 2
    return (f is MutableFoo && f.value == 2)
}
