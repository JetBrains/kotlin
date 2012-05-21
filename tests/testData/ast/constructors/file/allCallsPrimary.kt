open class C(arg1 : Int, arg2 : Int, arg3 : Int) {
class object {
open fun init(arg1 : Int, arg2 : Int) : C {
val __ = C(arg1, arg2, 0)
return __
}
open fun init(arg1 : Int) : C {
val __ = C(arg1, 0, 0)
return __
}
}
}
public open class User() {
class object {
public open fun main() : Unit {
var c1 : C? = C(100, 100, 100)
var c2 : C? = C.init(100, 100)
var c3 : C? = C.init(100)
}
}
}