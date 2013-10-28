public open class NonDefault() {
class object {
public open fun main(args : Array<String?>?) : Unit {
var value : Int = 3
var valueString : String? = ""
when (value) {
else -> {
}
}
System.out?.println(valueString)
}
}
}
fun main(args : Array<String>) = NonDefault.main(args as Array<String?>?)