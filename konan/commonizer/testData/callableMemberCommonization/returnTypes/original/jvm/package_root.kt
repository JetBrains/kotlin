class Planet(val name: String, val diameter: Double)

val propertyWithInferredType1 get() = 1
val propertyWithInferredType2 get() = "hello"
val propertyWithInferredType3 get() = 42.toString()
val propertyWithInferredType4 get() = null
val propertyWithInferredType5 get() = Planet("Earth", 12742)

typealias B = Planet
typealias C = Planet

// with explicit type:
val property1: Int = 1
val property2: String = "hello"
val property3: Planet = Planet("Earth", 12742)
val property4: B = Planet("Earth", 12742)
val property5: Planet = A("Earth", 12742)
val property6: Planet = A("Earth", 12742)
val property7: C = Planet("Earth", 12742)

// with explicit type:
fun function1(): Int = 1
fun function2(): String = "hello"
fun function3(): Planet = Planet("Earth", 12742)
fun function4(): B = A("Earth", 12742)
fun function5(): Planet = A("Earth", 12742)
fun function6(): Planet = Planet("Earth", 12742)
fun function7(): C = C("Earth", 12742)

val propertyWithMismatchedType1: Long = 1
val propertyWithMismatchedType2: Short = 1
val propertyWithMismatchedType3: Number = 1
val propertyWithMismatchedType4: Comparable<Int> = 1
val propertyWithMismatchedType5: String = 1.toString()

fun functionWithMismatchedType1(): Long = 1
fun functionWithMismatchedType2(): Short = 1
fun functionWithMismatchedType3(): Number = 1
fun functionWithMismatchedType4(): Comparable<Int> = 1
fun functionWithMismatchedType5(): String = 1.toString()
