package demo
class Test() {
fun test() {
val name = "$$$$"
name = name.replaceAll("\\$[0-9]+", "\\$")
val c = '$'
System.out.println(c)
val C = '$'
System.out.println(C)
}
}