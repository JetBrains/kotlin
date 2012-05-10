public open class Identifier(_myName : String?, _myHasDollar : Boolean) {
private val myName : String? = null
private var myHasDollar : Boolean = false
private var myNullable : Boolean = true
public open fun getName() : String? {
return myName
}
{
myName = _myName
myHasDollar = _myHasDollar
}
class object {
public open fun init(name : String?) : Identifier {
val __ = Identifier(name, false)
return __
}
public open fun init(name : String?, isNullable : Boolean) : Identifier {
val __ = Identifier(name, false)
__.myNullable = isNullable
return __
}
public open fun init(name : String?, hasDollar : Boolean, isNullable : Boolean) : Identifier {
val __ = Identifier(name, hasDollar)
__.myNullable = isNullable
return __
}
}
}
public open class User() {
class object {
public open fun main() : Unit {
var i1 : Identifier? = Identifier.init("name", false, true)
var i2 : Identifier? = Identifier.init("name", false)
var i3 : Identifier? = Identifier.init("name")
}
}
}