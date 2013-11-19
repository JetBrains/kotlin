package demo
class Container() {
var myInt : Int = 1
}
class One() {
class object {
var myContainer : Container = Container()
}
}
class IntContainer(i : Int) {
}
class Test() {
open fun putInt(i : Int) {
}
open fun test() {
putInt(One.myContainer.myInt)
IntContainer(One.myContainer.myInt)
}
}