// "Create type parameter 'X' in class 'Foo'" "true"
// ERROR: Type mismatch: inferred type is A<Int> but A<Any?> was expected
// COMPILER_ARGUMENTS: -XXLanguage:-NewInference
class A<T>

open class Foo(x: A<<caret>X>)

class Bar : Foo(A())

fun test() {
    Foo(A())
    Foo(A<Int>())

    object : Foo(A<Int>()) {

    }
}