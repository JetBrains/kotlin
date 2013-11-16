public open class NonDefault() {
class object {
public open fun main(args : Array<String>) : Unit {
val value = 3
val valueString = ""
when (value) {
1 -> {
valueString = "ONE"
}
2 -> {
valueString = "TWO"
}
3 -> {
valueString = "THREE"
}
else -> {
}
}
System.out.println(valueString)
}
}
}
fun main(args : Array<String>) = NonDefault.main(args as Array<String?>?)