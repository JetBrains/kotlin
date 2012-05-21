open class C(arg1 : Int) {
val myArg1 : Int
var myArg2 : Int = 0
var myArg3 : Int = 0
{
myArg1 = arg1
myArg2 = 0
myArg3 = 0
}
class object {
open fun init(arg1 : Int, arg2 : Int, arg3 : Int) : C {
val __ = C(arg1)
__.myArg2 = arg2
__.myArg3 = arg3
return __
}
open fun init(arg1 : Int, arg2 : Int) : C {
val __ = C(arg1)
__.myArg2 = arg2
__.myArg3 = 0
return __
}
}
}
public open class User() {
class object {
public open fun main() : Unit {
var c1 : C? = C.init(100, 100, 100)
var c2 : C? = C.init(100, 100)
var c3 : C? = C(100)
}
}
}