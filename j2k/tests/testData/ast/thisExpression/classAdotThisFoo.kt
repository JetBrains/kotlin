open class Base() {
open fun foo() {
}
}
open class A() : Base() {
open class C() {
open fun test() {
this@A.foo()
}
}
}