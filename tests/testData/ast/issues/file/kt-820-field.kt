package demo
open class Container() {
var myInt : Int = 1
}
open class One() {
class object {
var myContainer : Container? = Container()
}
}
open class Test() {
var b : Byte = One.myContainer?.myInt!!.toByte()
}