public open class Identifier<T>(myName : T?, myHasDollar : Boolean) {
private val myName : T?
private var myHasDollar : Boolean
private var myNullable : Boolean = true
open public fun getName() : T? {
return myName
}
class object {
open public fun init<T>(name : T?) : Identifier {
val __ = Identifier(name, false)
return __
}
open public fun init<T>(name : T?, isNullable : Boolean) : Identifier {
val __ = Identifier(name, false)
__.myNullable = isNullable
return __
}
open public fun init<T>(name : T?, hasDollar : Boolean, isNullable : Boolean) : Identifier {
val __ = Identifier(name, hasDollar)
__.myNullable = isNullable
return __
}
}
{
$myName = myName
$myHasDollar = myHasDollar
}
}
public open class User() {
class object {
open public fun main() : Unit {
var i1 : Identifier<*>? = Identifier.init<String?>("name", false, true)
var i2 : Identifier<*>? = Identifier.init<String?>("name", false)
var i3 : Identifier<*>? = Identifier.init<String?>("name")
}
}
}