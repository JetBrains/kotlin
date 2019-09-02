class Planet(val name: String, val diameter: Double)

actual val propertyWithInferredType1 = 1
actual val propertyWithInferredType2 = "hello"
actual val propertyWithInferredType3 = 42.toString()
actual val propertyWithInferredType4 = null
actual val propertyWithInferredType5 = Planet("Earth", 12742)

typealias A = Planet
typealias C = Planet

actual val property1 = 1
actual val property2 = "hello"
actual val property3 = Planet("Earth", 12742)
val property4 = A("Earth", 12742)
val property5 = A("Earth", 12742)
actual val property6 = Planet("Earth", 12742)
actual val property7 = C("Earth", 12742)

val propertyWithMismatchedType1: Int = 1
val propertyWithMismatchedType2: Int = 1
val propertyWithMismatchedType3: Int = 1
val propertyWithMismatchedType4: Int = 1
val propertyWithMismatchedType5: Int = 1
