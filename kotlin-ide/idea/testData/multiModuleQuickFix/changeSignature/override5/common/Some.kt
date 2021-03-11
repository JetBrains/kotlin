// "Convert parameter to receiver" "true"
expect open class A() {
    open fun c(a: Int, b: String)
}

class C : A() {
    override fun c(a: Int, <caret>b: String) {}
}