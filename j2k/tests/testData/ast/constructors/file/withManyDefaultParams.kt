public open class Test(_myName : String?, _a : Boolean, _b : Double, _c : Float, _d : Long, _e : Int, _f : Short, _g : Char) {
private val myName : String?
private var a : Boolean = false
private var b : Double = 0.toDouble()
private var c : Float = 0.toFloat()
private var d : Long = 0
private var e : Int = 0
private var f : Short = 0
private var g : Char = ' '
{
myName = _myName
a = _a
b = _b
c = _c
d = _d
e = _e
f = _f
g = _g
}
class object {
public open fun init() : Test {
val __ = Test(null, false, 0.toDouble(), 0.toFloat(), 0, 0, 0, ' ')
return __
}
public open fun init(name : String?) : Test {
val __ = Test(foo(name), false, 0.toDouble(), 0.toFloat(), 0, 0, 0, ' ')
return __
}
open fun foo(n : String?) : String? {
return ""
}
}
}
public open class User() {
class object {
public open fun main() : Unit {
var t : Test? = Test.init("name")
}
}
}