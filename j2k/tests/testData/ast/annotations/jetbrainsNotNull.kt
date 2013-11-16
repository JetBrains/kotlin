package test
public open class Test(str : String) {
var myStr : String = "String2"
public open fun sout(str : String) : Unit {
System.out?.println(str)
}
public open fun dummy(str : String) : String {
return str
}
public open fun test() : Unit {
sout("String")
var test : String = "String2"
sout(test)
sout(dummy(test))
Test(test)
}
{
myStr = str
}
}