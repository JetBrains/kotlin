package demo
open class Test() {
open fun putInt(i : Int) : Unit {
}
open fun test() : Unit {
var b : Byte = 10
putInt(b.toInt())
}
}