package demo
open class Container() {
var myInt : Int = 1
}
open class One() {
class object {
var myContainer : Container? = Container()
}
}
open class IntContainer(i : Int) {
}
open class Test() {
open fun putInt(i : Int) : Unit {
}
open fun test() : Unit {
putInt((One.myContainer?.myInt).sure())
IntContainer((One.myContainer?.myInt).sure())
}
}