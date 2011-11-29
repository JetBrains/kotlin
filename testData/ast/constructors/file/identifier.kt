public open class Identifier(myName : String?, myHasDollar : Boolean) {
{
$myName = myName
$myHasDollar = myHasDollar
}
private val myName : String? = null
private var myHasDollar : Boolean = false
private var myNullable : Boolean = true
open public fun getName() : String? {
return myName
}
class object {
open public fun init(name : String?) : Identifier {
val __ = Identifier(name, false)
return __
}
open public fun init(name : String?, isNullable : Boolean) : Identifier {
val __ = Identifier(name, false)
__.myNullable = isNullable
return __
}
open public fun init(name : String?, hasDollar : Boolean, isNullable : Boolean) : Identifier {
val __ = Identifier(name, hasDollar)
__.myNullable = isNullable
return __
}
}
}
public open class User() {
class object {
open public fun main() : Unit {
var i1 : Identifier? = Identifier.init("name", false, true)
var i2 : Identifier? = Identifier.init("name", false)
var i3 : Identifier? = Identifier.init("name")
}
}
}