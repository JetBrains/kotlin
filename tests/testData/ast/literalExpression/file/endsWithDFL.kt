open class Test() {
open fun test() : Unit {
var l1 : Long = 10
var d1 : Double = 10.0
var f1 : Float = 10.0.toFloat()
var l2 : Long = 10
var d2 : Double = 10.0
var f2 : Float = 10.0.toFloat()
}
open fun testBoxed() : Unit {
var l1 : Long? = 10
var d1 : Double? = 10.0
var f1 : Float? = 10.0.toFloat()
var l2 : Long? = 10
var d2 : Double? = 10.0
var f2 : Float? = 10.0.toFloat()
}
}