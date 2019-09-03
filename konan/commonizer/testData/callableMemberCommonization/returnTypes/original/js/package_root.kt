class Planet(val name: String, val diameter: Double)

val propertyWithInferredType1 = 1
val propertyWithInferredType2 = "hello"
val propertyWithInferredType3 = 42.toString()
val propertyWithInferredType4 = null
val propertyWithInferredType5 = Planet("Earth", 12742)

typealias A = Planet
typealias C = Planet

// with inferred type:
val property1 = 1
val property2 = "hello"
val property3 = Planet("Earth", 12742)
val property4 = A("Earth", 12742)
val property5 = A("Earth", 12742)
val property6 = Planet("Earth", 12742)
val property7 = C("Earth", 12742)

// with inferred type:
fun function1() = 1
fun function2() = "hello"
fun function3() = Planet("Earth", 12742)
fun function4() = A("Earth", 12742)
fun function5() = A("Earth", 12742)
fun function6() = Planet("Earth", 12742)
fun function7() = C("Earth", 12742)

val propertyWithMismatchedType1: Int = 1
val propertyWithMismatchedType2: Int = 1
val propertyWithMismatchedType3: Int = 1
val propertyWithMismatchedType4: Int = 1
val propertyWithMismatchedType5: Int = 1

fun functionWithMismatchedType1(): Int = 1
fun functionWithMismatchedType2(): Int = 1
fun functionWithMismatchedType3(): Int = 1
fun functionWithMismatchedType4(): Int = 1
fun functionWithMismatchedType5(): Int = 1
