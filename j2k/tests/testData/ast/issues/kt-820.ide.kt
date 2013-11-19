package demo
class Container() {
var myInt : Int = 1
}
class One() {
class object {
var myContainer : Container = Container()
}
}
class Test() {
open fun test() {
val b = One.myContainer.myInt.toByte()
}
}