package demo
open class Container() {
var myString : String? = "1"
}
open class One() {
class object {
var myContainer : Container? = Container()
}
}
open class StringContainer(s : String?) {
}
open class Test() {
open fun putString(s : String?) : Unit {
}
open fun test() : Unit {
putString(One.myContainer?.myString)
StringContainer(One.myContainer?.myString)
}
}