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
fun putInt(i : Int) {
}
fun test() {
putInt(One.myContainer.myInt)
IntContainer(One.myContainer.myInt)
}
}