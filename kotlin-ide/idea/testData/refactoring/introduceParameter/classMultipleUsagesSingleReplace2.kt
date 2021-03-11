// SINGLE_REPLACE
// TARGET:
class A(val a: Int, s: String) {
    val x = <selection>a + 1</selection>

    fun foo() = (a + 1) * 2
}

fun test() {
    A(1, "2")
}