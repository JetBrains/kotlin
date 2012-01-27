package demo
import java.util.HashMap
open class Test() {
class object {
open fun init() : Test {
val __ = Test()
return __
}
open fun init(s : String?) : Test {
val __ = Test()
return __
}
}
}
open class User() {
open fun main() : Unit {
var m : HashMap<Any?, Any?>? = HashMap(1)
var m2 : HashMap<Any?, Any?>? = HashMap(10)
var t1 : Test? = Test.init()
var t2 : Test? = Test.init("")
}
}