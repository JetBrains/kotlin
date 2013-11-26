class Base() {
    fun foo()
}
class A() : Base() {
    class C() {
        fun test() {
            super@A.foo()
        }
    }
}