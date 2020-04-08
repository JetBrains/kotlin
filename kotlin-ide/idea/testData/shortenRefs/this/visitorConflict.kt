package test

fun foo(n: Int) {

}

class A(val n: Int) {
    fun test() {
        <selection>test.foo(this@A)</selection>
    }
}