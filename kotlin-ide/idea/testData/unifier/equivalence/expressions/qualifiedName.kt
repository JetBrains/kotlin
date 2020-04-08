// ERROR: Unresolved reference: B
package p.q.r

class A {
    class B {
        val n = 1

        fun foo() {
            <selection>B().n</selection>
            A.B().n
            p.q.r.A.B().n
        }
    }
}

fun foo() {
    B().n
    A.B().n
    p.q.r.A.B().n
}