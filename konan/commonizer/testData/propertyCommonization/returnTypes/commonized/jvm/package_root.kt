class Planet(val name: String, val diameter: Double)

actual val propertyWithInferredType1 get() = 1
actual val propertyWithInferredType2 get() = "hello"
actual val propertyWithInferredType3 get() = 42.toString()
actual val propertyWithInferredType4 get() = null
actual val propertyWithInferredType5 get() = Planet("Earth", 12742)

typealias B = Planet
typealias C = Planet

actual val property1 = 1
actual val property2 = "hello"
actual val property3 = Planet("Earth", 12742)
val property4: B = Planet("Earth", 12742)
val property5: Planet = A("Earth", 12742)
actual val property6: Planet = A("Earth", 12742)
actual val property7: C = Planet("Earth", 12742)

val propertyWithMismatchedType1: Long = 1
val propertyWithMismatchedType2: Short = 1
val propertyWithMismatchedType3: Number = 1
val propertyWithMismatchedType4: Comparable<Int> = 1
val propertyWithMismatchedType5: String = 1.toString()
