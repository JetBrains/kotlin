fun <T> doSomething(a: T) {}

class Foo {
    val b: String?
        get() {
            return "Foo"
        }
}

fun main(args: Array<String>) {
    val a = Foo()
    doSomething(a.b?.<caret>length)
}
