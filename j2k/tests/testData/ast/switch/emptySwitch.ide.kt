public open class NonDefault() {
class object {
public open fun main(args : Array<String>) : Unit {
val value = 3
val valueString = ""
when (value) {
else -> {
}
}
System.out.println(valueString)
}
}
}
fun main(args : Array<String>) = NonDefault.main(args as Array<String?>?)