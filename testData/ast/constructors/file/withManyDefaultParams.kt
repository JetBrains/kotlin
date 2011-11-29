public open class Test(myName : String?, a : Boolean, b : Double, c : Float, d : Long, e : Int, f : Short, g : Char) {
{
$myName = myName
$a = a
$b = b
$c = c
$d = d
$e = e
$f = f
$g = g
}
private val myName : String? = null
private var a : Boolean = false
private var b : Double = 0.dbl
private var c : Float = 0.flt
private var d : Long = 0
private var e : Int = 0
private var f : Short = 0
private var g : Char = ' '
class object {
open public fun init() : Test {
val __ = Test(null, false, 0.dbl, 0.flt, 0, 0, 0, ' ')
return __
}
open public fun init(name : String?) : Test {
val __ = Test(foo(name), false, 0.dbl, 0.flt, 0, 0, 0, ' ')
return __
}
open fun foo(n : String?) : String? {
return ""
}
}
}
public open class User() {
class object {
open public fun main() : Unit {
var t : Test? = Test.init("name")
}
}
}