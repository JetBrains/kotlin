package demo
open class Test() {
open fun test() {
val name = "$$$$"
name = name.replaceAll("\\$[0-9]+", "\\$")
val c = '$'
System.out.println(c)
val C = '$'
System.out.println(C)
}
}