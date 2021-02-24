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

class Box<T>(val value: T)
class Fox

fun functionWithTypeParametersInReturnType1() = arrayOf(1)
fun functionWithTypeParametersInReturnType2() = arrayOf("hello")
fun functionWithTypeParametersInReturnType3() = arrayOf("hello")
fun functionWithTypeParametersInReturnType4(): List<Int> = listOf(1)
fun functionWithTypeParametersInReturnType5(): List<String> = listOf("hello")
fun functionWithTypeParametersInReturnType6(): List<String> = listOf("hello")
fun functionWithTypeParametersInReturnType7() = Box(1)
fun functionWithTypeParametersInReturnType8() = Box("hello")
fun functionWithTypeParametersInReturnType9() = Box("hello")
fun functionWithTypeParametersInReturnType10() = Box(Planet("Earth", 12742))
fun functionWithTypeParametersInReturnType11() = Box(Fox())
fun functionWithTypeParametersInReturnType12() = Box(Fox())

fun <T> functionWithUnsubstitutedTypeParametersInReturnType1(): T = TODO()
fun <T : Any?> functionWithUnsubstitutedTypeParametersInReturnType2(): T = TODO()
fun <T : Any> functionWithUnsubstitutedTypeParametersInReturnType3(): T = TODO()
fun <Q> functionWithUnsubstitutedTypeParametersInReturnType4(): Q = TODO()
fun <T, Q> functionWithUnsubstitutedTypeParametersInReturnType5(): T = TODO()
fun <T, Q> functionWithUnsubstitutedTypeParametersInReturnType6(): Q = TODO()
fun functionWithUnsubstitutedTypeParametersInReturnType7(): String = TODO()
fun <T> functionWithUnsubstitutedTypeParametersInReturnType8(): Box<T> = TODO()
fun functionWithUnsubstitutedTypeParametersInReturnType9(): Box<String> = TODO()

class Outer<A> {
    class Nested<B> {
        class Nested<C>
        inner class Inner<D>
    }
    inner class Inner<E> {
        inner class Inner<F>
    }
}

fun <T> returnOuter(): Outer<T> = TODO()
fun <T> returnOuterNested(): Outer.Nested<T> = TODO()
fun <T> returnOuterNestedNested(): Outer.Nested.Nested<T> = TODO()
fun <T, R> returnOuterInner(): Outer<T>.Inner<R> = TODO()
fun <T, R, S> returnOuterInnerInner(): Outer<T>.Inner<R>.Inner<S> = TODO()
