// IGNORE_BACKEND: JS

package foo

class A(val v: String) {
    fun foo() = v
}

fun box(): String {
    val aRef1 = A::foo
    val aRef2 = A::foo
    if (aRef1 !== aRef2) return "Fail != "
    return aRef1(A("O"))  + aRef2(A("K"))
}
