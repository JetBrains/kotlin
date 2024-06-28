interface I {
    fun foo()
}

class A(i: I): I by i {

}