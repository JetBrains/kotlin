package demo
public open class SwitchDemo() {
class object {
public open fun print(o : Any?) : Unit {
System.out?.println(o)
}
public open fun test(i : Int) : Unit {
var monthString : String? = "<empty>"
when (i) {
1 -> {
print(1)
print(2)
print(3)
print(4)
print(5)
}
2 -> {
print(2)
print(3)
print(4)
print(5)
}
3 -> {
print(3)
print(4)
print(5)
}
4 -> {
print(4)
print(5)
}
5 -> {
print(5)
}
6 -> {
print(6)
print(7)
print(8)
print(9)
print(10)
print(11)
monthString = "December"
}
7 -> {
print(7)
print(8)
print(9)
print(10)
print(11)
monthString = "December"
}
8 -> {
print(8)
print(9)
print(10)
print(11)
monthString = "December"
}
9 -> {
print(9)
print(10)
print(11)
monthString = "December"
}
10 -> {
print(10)
print(11)
monthString = "December"
}
11 -> {
print(11)
monthString = "December"
}
12 -> {
monthString = "December"
}
else -> {
monthString = "Invalid month"
}
}
System.out?.println(monthString)
}
public open fun main(args : Array<String?>?) : Unit {
for (i in 1..12) test(i)
}
}
}
fun main(args : Array<String?>?) = SwitchDemo.main(args)