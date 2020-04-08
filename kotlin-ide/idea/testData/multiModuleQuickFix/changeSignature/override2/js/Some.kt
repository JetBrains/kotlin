//
actual open class A {
    actual open fun c(a: Int, b: String) {}
}

class B : A() {
    override fun c(a: Int, b: String) {}
}