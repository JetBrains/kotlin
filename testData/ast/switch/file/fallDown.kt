namespace switch_demo
public open class SwitchDemo() {
class object {
open public fun main(args : Array<String?>?) : Unit {
var month : Int = 8
var monthString : String?
when (month) {
1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 => {
monthString = "December"
}
else => {
monthString = "Invalid month"
}
}
System.out?.println(monthString)
}
}
}