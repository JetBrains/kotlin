namespace test
public open class Test(str : String) {
{
myStr = str
}
var myStr : String? = "String2"
open public fun sout(str : String) : Unit {
System.out?.println(str)
}
open public fun dummy(str : String) : String {
return str
}
open public fun test() : Unit {
sout("String")
var test : String = "String2"
sout(test)
sout(dummy(test))
Test(test)
}
}