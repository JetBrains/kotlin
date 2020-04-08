open class Foo : Throwable()

val foo: Foo = Foo()
val foo1: Foo = Foo()

val foos: List<Foo> = listOf()
val foos1: Array<Foo> = array()

fun main(args: Array<String>) {
    val foo: Foo = Foo()
    val someVerySpecialFoo: Foo = Foo()
    val fooAnother: Foo = Foo()

    val anonymous = object : Foo() {
    }

    val (foo1: Foo, foos: List<Foo>) = Pair(Foo(), listOf<Foo>())

    try {
        for (foo2: Foo in listOf<Foo>()) {

        }
    } catch (foo: Foo) {

    }

    fun local(foo: Foo) {

    }
}

fun topLevel(foo: Foo) {

}

fun collectionLikes(foos: List<Array<Foo>>, foos: List<Map<Foo, Foo>>) {

}

class FooImpl : Foo()

object FooObj : Foo()