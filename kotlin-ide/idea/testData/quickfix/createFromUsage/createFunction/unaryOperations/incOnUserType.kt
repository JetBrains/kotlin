// "Create member function 'A.inc'" "true"

class A<T>(val n: T)

fun test() {
    var a = A(1)
    a<caret>++
}