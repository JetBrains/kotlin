class A() {
open fun foo() {
}
}
class B() : A() {
override fun foo() {
}
}
class C() : B() {
override fun foo() {
}
}