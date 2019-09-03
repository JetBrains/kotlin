class Planet(val name: String, val diameter: Double)

val intProperty get() = 42
val Int.intProperty get() = this
val Short.intProperty get() = toInt()
val Long.intProperty get() = toInt()
val String.intProperty get() = length
val Planet.intProperty get() = diameter.toInt()

fun intFunction() = 42
fun Int.intFunction() = this
fun Short.intFunction() = toInt()
fun Long.intFunction() = toInt()
fun String.intFunction() = length
fun Planet.intFunction() = diameter.toInt()

val mismatchedProperty1 get() = 42
val Double.mismatchedProperty2 get() = 42

fun mismatchedFunction1() = 42
fun Double.mismatchedFunction2() = 42
