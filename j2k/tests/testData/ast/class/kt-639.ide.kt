package demo
import java.util.HashMap
open class Test() {
class object {
open fun init() : Test {
val __ = Test()
return __
}
open fun init(s : String) : Test {
val __ = Test()
return __
}
}
}
open class User() {
open fun main() {
val m = HashMap(1)
val m2 = HashMap(10)
val t1 = Test.init()
val t2 = Test.init("")
}
}