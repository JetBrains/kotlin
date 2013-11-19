open class Base() {
open fun foo()
}
open class A() : Base() {
open class C() {
open fun test() {
super@A.foo()
}
}
}