class Planet(val name: String, val diameter: Double)

actual val intProperty get() = 42
actual val Int.intProperty get() = this
actual val Short.intProperty get() = toInt()
actual val Long.intProperty get() = toInt()
actual val String.intProperty get() = length
actual val Planet.intProperty get() = diameter.toInt()

val String.mismatchedProperty1 get() = 42
val mismatchedProperty2 get() = 42
