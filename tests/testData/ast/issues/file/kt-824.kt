package demo
open class Container() {
var myBoolean : Boolean = true
}
open class One() {
class object {
var myContainer : Container? = Container()
}
}
open class Test() {
open fun test() : Unit {
if (One.myContainer?.myBoolean.sure())
System.out?.println("Ok")
var s : String? = (if (One.myContainer?.myBoolean.sure())
"YES"
else
"NO")
while (One.myContainer?.myBoolean.sure())
System.out?.println("Ok")
do
{
System.out?.println("Ok")
}
while (One.myContainer?.myBoolean.sure())
}
}