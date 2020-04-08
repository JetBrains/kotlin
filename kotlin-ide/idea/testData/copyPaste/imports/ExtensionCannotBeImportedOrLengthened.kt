// RUNTIME
package a

class A() {
}

fun A.ext() {
}

var A.p: Int
    get() = 2
    set(i: Int) = throw UnsupportedOperationException()

<selection>fun A.f() {
    ext()
    p
    p = 3
}</selection>