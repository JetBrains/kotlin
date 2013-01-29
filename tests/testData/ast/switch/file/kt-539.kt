package switch_demo
public open class SwitchDemo() {
class object {
public open fun main(args : Array<String?>?) : Unit {
var month : Int = 8
var monthString : String?
when (month) {
1 -> {
monthString = "January"
}
2 -> {
monthString = "February"
}
3 -> {
monthString = "March"
}
4 -> {
monthString = "April"
}
5 -> {
monthString = "May"
}
6 -> {
monthString = "June"
}
7 -> {
monthString = "July"
}
8 -> {
monthString = "August"
}
9 -> {
monthString = "September"
}
10 -> {
monthString = "October"
}
11 -> {
monthString = "November"
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
}
}
fun main(args : Array<String?>?) = SwitchDemo.main(args)