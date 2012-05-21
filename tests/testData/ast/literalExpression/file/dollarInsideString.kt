package demo
open class Test() {
open fun test() : Unit {
var name : String? = "$$$$"
name = name?.replaceAll("\\$[0-9]+", "\\$")
var c : Char = '$'
System.out?.println(c)
var C : Char? = '$'
System.out?.println(C)
}
}