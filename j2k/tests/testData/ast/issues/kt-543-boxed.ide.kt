package demo
open class Test() {
open fun putInt(i : Int) : Unit {
}
open fun test() : Unit {
val b = 10
putInt(b.toInt())
val b2 = 10
putInt(b2.toInt())
}
}