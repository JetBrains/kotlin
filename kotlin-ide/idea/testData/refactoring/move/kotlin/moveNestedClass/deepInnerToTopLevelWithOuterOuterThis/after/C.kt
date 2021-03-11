package test

class C(private val a: A.B) {
    fun test() {
        OuterOuterY()
        this@A.OuterOuterY()
    }
}