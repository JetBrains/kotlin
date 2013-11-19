class Base() {
open fun foo() {
}
}
class A() : Base() {
class C() {
open fun test() {
this@A.foo()
}
}
}