// !DIAGNOSTICS_NUMBER: 3
// !DIAGNOSTICS: INVISIBLE_MEMBER

package foo.bar

class A {
    private class B
    public class C private()

    private fun bar() {}
}

fun foo() {
    A.B()     // ERROR 1: Cannot access 'B': it is private in 'A'
    A.C()     // ERROR 2: Cannot access '' : it is private in 'C'

    A().bar() // ERROR 3: Cannot access 'bar' : it is private in 'A'
}
