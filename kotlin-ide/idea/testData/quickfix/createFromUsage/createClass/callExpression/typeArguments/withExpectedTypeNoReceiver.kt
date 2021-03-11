// "Create class 'Foo'" "true"

open class A {

}

fun test(a: A): A = <caret>Foo<A, Int>(a, 1)