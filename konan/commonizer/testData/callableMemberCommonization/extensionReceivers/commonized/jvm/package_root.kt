actual class Planet actual constructor(actual val name: String, actual val diameter: Double)

actual val intProperty get() = 42
actual val Int.intProperty get() = this
actual val Short.intProperty get() = toInt()
actual val Long.intProperty get() = toInt()
actual val String.intProperty get() = length
actual val Planet.intProperty get() = diameter.toInt()

actual fun intFunction() = 42
actual fun Int.intFunction() = this
actual fun Short.intFunction() = toInt()
actual fun Long.intFunction() = toInt()
actual fun String.intFunction() = length
actual fun Planet.intFunction() = diameter.toInt()

val mismatchedProperty1 get() = 42
val Double.mismatchedProperty2 get() = 42

fun mismatchedFunction1() = 42
fun Double.mismatchedFunction2() = 42

actual val <T> T.propertyWithTypeParameter1 get() = 42
actual val <T : Any?> T.propertyWithTypeParameter2 get() = 42
val <T : Any> T.propertyWithTypeParameter3 get() = 42
actual val <T : CharSequence> T.propertyWithTypeParameter4 get() = length
val <T : Appendable> T.propertyWithTypeParameter5 get() = length
val <T : String> T.propertyWithTypeParameter6 get() = length
val String.propertyWithTypeParameter7 get() = length
val <Q> Q.propertyWithTypeParameter8 get() = 42
val <T, Q> T.propertyWithTypeParameter9 get() = 42

actual fun <T> T.functionWithTypeParameter1() {}
fun <Q> Q.functionWithTypeParameter2() {}
fun <T, Q> T.functionWithTypeParameter3() {}
fun <T, Q> Q.functionWithTypeParameter4() {}
