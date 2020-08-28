actual class Planet actual constructor(actual val name: String, actual val diameter: Double)

actual val propertyWithInferredType1 get() = 1
actual val propertyWithInferredType2 get() = "hello"
actual val propertyWithInferredType3 get() = 42.toString()
actual val propertyWithInferredType4 get() = null
actual val propertyWithInferredType5 get() = Planet("Earth", 12742)

typealias B = Planet

actual val property1 = 1
actual val property2 = "hello"
actual val property3 = Planet("Earth", 12742)
val property4: B = Planet("Earth", 12742)
val property5: Planet = A("Earth", 12742)
actual val property6: Planet = A("Earth", 12742)
actual val property7: C = Planet("Earth", 12742)

actual fun function1(): Int = 1
actual fun function2(): String = "hello"
actual fun function3(): Planet = Planet("Earth", 12742)
fun function4(): B = A("Earth", 12742)
fun function5(): Planet = A("Earth", 12742)
actual fun function6(): Planet = Planet("Earth", 12742)
actual fun function7(): C = C("Earth", 12742)

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

actual class Box<T> actual constructor(actual val value: T)
actual class Fox actual constructor()

actual fun functionWithTypeParametersInReturnType1() = arrayOf(1)
fun functionWithTypeParametersInReturnType2() = arrayOf("hello")
actual fun functionWithTypeParametersInReturnType3() = arrayOf("hello")
actual fun functionWithTypeParametersInReturnType4(): List<Int> = listOf(1)
fun functionWithTypeParametersInReturnType5(): List<String> = listOf("hello")
actual fun functionWithTypeParametersInReturnType6(): List<String> = listOf("hello")
actual fun functionWithTypeParametersInReturnType7() = Box(1)
fun functionWithTypeParametersInReturnType8() = Box("hello")
actual fun functionWithTypeParametersInReturnType9() = Box("hello")
actual fun functionWithTypeParametersInReturnType10() = Box(Planet("Earth", 12742))
fun functionWithTypeParametersInReturnType11() = Box(Fox())
actual fun functionWithTypeParametersInReturnType12() = Box(Fox())

actual fun <T> functionWithUnsubstitutedTypeParametersInReturnType1(): T = TODO()
actual fun <T : Any?> functionWithUnsubstitutedTypeParametersInReturnType2(): T = TODO()
fun <T : Any> functionWithUnsubstitutedTypeParametersInReturnType3(): T = TODO()
fun <Q> functionWithUnsubstitutedTypeParametersInReturnType4(): Q = TODO()
fun <T, Q> functionWithUnsubstitutedTypeParametersInReturnType5(): T = TODO()
fun <T, Q> functionWithUnsubstitutedTypeParametersInReturnType6(): Q = TODO()
fun functionWithUnsubstitutedTypeParametersInReturnType7(): String = TODO()
actual fun <T> functionWithUnsubstitutedTypeParametersInReturnType8(): Box<T> = TODO()
fun functionWithUnsubstitutedTypeParametersInReturnType9(): Box<String> = TODO()
