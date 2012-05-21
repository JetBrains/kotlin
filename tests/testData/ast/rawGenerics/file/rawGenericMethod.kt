package demo
open class TestT() {
open fun getT<T>() : Unit {
}
}
open class U() {
open fun main() : Unit {
var t : TestT? = TestT()
t?.getT<String?>()
t?.getT<Int?>()
t?.getT()
}
}