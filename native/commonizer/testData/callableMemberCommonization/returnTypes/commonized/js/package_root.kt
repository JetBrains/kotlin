actual class Planet actual constructor(actual val name: String, actual val diameter: Double)

actual val propertyWithInferredType1 = 1
actual val propertyWithInferredType2 = "hello"
actual val propertyWithInferredType3 = 42.toString()
actual val propertyWithInferredType4 = null
actual val propertyWithInferredType5 = Planet("Earth", 12742)

typealias A = Planet

actual val property1 = 1
actual val property2 = "hello"
actual val property3 = Planet("Earth", 12742)
val property4 = A("Earth", 12742)
val property5 = A("Earth", 12742)
actual val property6 = Planet("Earth", 12742)
actual val property7 = C("Earth", 12742)

actual fun function1() = 1
actual fun function2() = "hello"
actual fun function3() = Planet("Earth", 12742)
fun function4() = A("Earth", 12742)
fun function5() = A("Earth", 12742)
actual fun function6() = Planet("Earth", 12742)
actual fun function7() = C("Earth", 12742)

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

actual class Box<T> actual constructor(actual val value: T)
actual class Fox actual constructor()

actual fun functionWithTypeParametersInReturnType1() = arrayOf(1)
fun functionWithTypeParametersInReturnType2() = arrayOf(1)
actual fun functionWithTypeParametersInReturnType3() = arrayOf("hello")
actual fun functionWithTypeParametersInReturnType4(): List<Int> = listOf(1)
fun functionWithTypeParametersInReturnType5(): List<Int> = listOf(1)
actual fun functionWithTypeParametersInReturnType6(): List<String> = listOf("hello")
actual fun functionWithTypeParametersInReturnType7() = Box(1)
fun functionWithTypeParametersInReturnType8() = Box(1)
actual fun functionWithTypeParametersInReturnType9() = Box("hello")
actual fun functionWithTypeParametersInReturnType10() = Box(Planet("Earth", 12742))
fun functionWithTypeParametersInReturnType11() = Box(Planet("Earth", 12742))
actual fun functionWithTypeParametersInReturnType12() = Box(Fox())

actual fun <T> functionWithUnsubstitutedTypeParametersInReturnType1(): T = TODO()
actual fun <T> functionWithUnsubstitutedTypeParametersInReturnType2(): T = TODO()
fun <T> functionWithUnsubstitutedTypeParametersInReturnType3(): T = TODO()
fun <T> functionWithUnsubstitutedTypeParametersInReturnType4(): T = TODO()
fun <T> functionWithUnsubstitutedTypeParametersInReturnType5(): T = TODO()
fun <T> functionWithUnsubstitutedTypeParametersInReturnType6(): T = TODO()
fun <T> functionWithUnsubstitutedTypeParametersInReturnType7(): T = TODO()
actual fun <T> functionWithUnsubstitutedTypeParametersInReturnType8(): Box<T> = TODO()
fun <T> functionWithUnsubstitutedTypeParametersInReturnType9(): Box<T> = TODO()
