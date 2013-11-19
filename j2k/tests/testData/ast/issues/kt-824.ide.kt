package demo
class Container() {
var myBoolean : Boolean = true
}
class One() {
class object {
var myContainer : Container = Container()
}
}
class Test() {
open fun test() {
if (One.myContainer.myBoolean)
System.out.println("Ok")
val s = (if (One.myContainer.myBoolean)
"YES"
else
"NO")
while (One.myContainer.myBoolean)
System.out.println("Ok")
do
{
System.out.println("Ok")
}
while (One.myContainer.myBoolean)
}
}