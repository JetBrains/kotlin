// "Create class 'Foo'" "true"

class A<T>(val n: T) {
    fun test() = this.<caret>Foo(2, "2")
}