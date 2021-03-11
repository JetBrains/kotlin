// "Create member function 'A.foo'" "true"
// ERROR: Unresolved reference: s

class A<T>(val n: T)

fun test(): Int {
    return A(1).<caret>foo(s, 1)
}