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

val <T> T.propertyWithTypeParameter1 get() = 42
val <T : Any?> T.propertyWithTypeParameter2 get() = 42
val <T : Any> T.propertyWithTypeParameter3 get() = 42
val <T : CharSequence> T.propertyWithTypeParameter4: Int get() = length
val <T : Appendable> T.propertyWithTypeParameter5: Int get() = length
val <T : String> T.propertyWithTypeParameter6: Int get() = length
val String.propertyWithTypeParameter7: Int get() = length
val <Q> Q.propertyWithTypeParameter8 get() = 42
val <T, Q> T.propertyWithTypeParameter9 get() = 42

fun <T> T.functionWithTypeParameter1() {}
fun <Q> Q.functionWithTypeParameter2() {}
fun <T, Q> T.functionWithTypeParameter3() {}
fun <T, Q> Q.functionWithTypeParameter4() {}
