class Planet(val name: String, val diameter: Double)

val intProperty get() = 42
val Int.intProperty get() = this
val Short.intProperty get() = toInt()
val Long.intProperty get() = toInt()
val String.intProperty get() = length
val Planet.intProperty get() = diameter.toInt()

val String.mismatchedProperty1 get() = 42
val mismatchedProperty2 get() = 42
