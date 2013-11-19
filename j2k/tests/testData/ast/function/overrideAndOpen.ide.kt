open class A() {
open fun foo() {
}
}
open class B() : A() {
override fun foo() {
}
}
open class C() : B() {
override fun foo() {
}
}