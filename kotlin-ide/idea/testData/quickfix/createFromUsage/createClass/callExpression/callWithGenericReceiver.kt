// "Create class 'Foo'" "true"

class A<T>(val n: T) {

}

fun <U> test(u: U) {
    val a = A(u).<caret>Foo(u)
}