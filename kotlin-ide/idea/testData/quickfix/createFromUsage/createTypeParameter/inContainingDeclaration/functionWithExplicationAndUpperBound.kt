// "Create type parameter 'X' in function 'foo'" "true"
// COMPILER_ARGUMENTS: -XXLanguage:-NewInference
class A<T : List<Int>>

fun foo(x: A<<caret>X>) {

}

fun test() {
    foo(A())
    foo(A<List<Int>>())
}