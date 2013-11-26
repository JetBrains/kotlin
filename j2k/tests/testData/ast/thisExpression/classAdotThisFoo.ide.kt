class Base() {
    fun foo() {
    }
}
class A() : Base() {
    class C() {
        fun test() {
            this@A.foo()
        }
    }
}