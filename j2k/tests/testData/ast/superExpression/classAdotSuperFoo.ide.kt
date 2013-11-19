class Base() {
open fun foo()
}
class A() : Base() {
class C() {
open fun test() {
super@A.foo()
}
}
}