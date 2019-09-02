class Planet(val name: String, val diameter: Double)

val propertyWithInferredType1 get() = 1
val propertyWithInferredType2 get() = "hello"
val propertyWithInferredType3 get() = 42.toString()
val propertyWithInferredType4 get() = null
val propertyWithInferredType5 get() = Planet("Earth", 12742)

typealias B = Planet
typealias C = Planet

val property1: Int = 1 // with explicit type
val property2: String = "hello" // with explicit type
val property3: Planet = Planet("Earth", 12742) // with explicit type
val property4: B = Planet("Earth", 12742) // with explicit type
val property5: Planet = A("Earth", 12742) // with explicit type
val property6: Planet = A("Earth", 12742) // with explicit type
val property7: C = Planet("Earth", 12742) // with explicit type

val propertyWithMismatchedType1: Long = 1
val propertyWithMismatchedType2: Short = 1
val propertyWithMismatchedType3: Number = 1
val propertyWithMismatchedType4: Comparable<Int> = 1
val propertyWithMismatchedType5: String = 1.toString()
