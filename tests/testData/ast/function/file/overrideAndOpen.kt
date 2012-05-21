open class A() {
open fun foo() : Unit {
}
}
open class B() : A() {
override fun foo() : Unit {
}
}
open class C() : B() {
override fun foo() : Unit {
}
}