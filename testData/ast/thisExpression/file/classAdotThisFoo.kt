namespace {
open class Base {
fun foo() : Unit {
}
}
open class A : Base {
open class C {
fun test() : Unit {
this@A.foo()
}
}
}
}