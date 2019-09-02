class Planet(val name: String, val diameter: Double)

val propertyWithInferredType1 = 1
val propertyWithInferredType2 = "hello"
val propertyWithInferredType3 = 42.toString()
val propertyWithInferredType4 = null
val propertyWithInferredType5 = Planet("Earth", 12742)

typealias A = Planet
typealias C = Planet

val property1 = 1 // with inferred type
val property2 = "hello" // with inferred type
val property3 = Planet("Earth", 12742) // with inferred type
val property4 = A("Earth", 12742) // with inferred type
val property5 = A("Earth", 12742) // with inferred type
val property6 = Planet("Earth", 12742) // with inferred type
val property7 = C("Earth", 12742) // with inferred type

val propertyWithMismatchedType1: Int = 1
val propertyWithMismatchedType2: Int = 1
val propertyWithMismatchedType3: Int = 1
val propertyWithMismatchedType4: Int = 1
val propertyWithMismatchedType5: Int = 1
