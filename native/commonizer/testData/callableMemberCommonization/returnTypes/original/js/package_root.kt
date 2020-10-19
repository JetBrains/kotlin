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

class Box<T>(val value: T)
class Fox

fun functionWithTypeParametersInReturnType1() = arrayOf(1)
fun functionWithTypeParametersInReturnType2() = arrayOf(1)
fun functionWithTypeParametersInReturnType3() = arrayOf("hello")
fun functionWithTypeParametersInReturnType4(): List<Int> = listOf(1)
fun functionWithTypeParametersInReturnType5(): List<Int> = listOf(1)
fun functionWithTypeParametersInReturnType6(): List<String> = listOf("hello")
fun functionWithTypeParametersInReturnType7() = Box(1)
fun functionWithTypeParametersInReturnType8() = Box(1)
fun functionWithTypeParametersInReturnType9() = Box("hello")
fun functionWithTypeParametersInReturnType10() = Box(Planet("Earth", 12742))
fun functionWithTypeParametersInReturnType11() = Box(Planet("Earth", 12742))
fun functionWithTypeParametersInReturnType12() = Box(Fox())

fun <T> functionWithUnsubstitutedTypeParametersInReturnType1(): T = TODO()
fun <T> functionWithUnsubstitutedTypeParametersInReturnType2(): T = TODO()
fun <T> functionWithUnsubstitutedTypeParametersInReturnType3(): T = TODO()
fun <T> functionWithUnsubstitutedTypeParametersInReturnType4(): T = TODO()
fun <T> functionWithUnsubstitutedTypeParametersInReturnType5(): T = TODO()
fun <T> functionWithUnsubstitutedTypeParametersInReturnType6(): T = TODO()
fun <T> functionWithUnsubstitutedTypeParametersInReturnType7(): T = TODO()
fun <T> functionWithUnsubstitutedTypeParametersInReturnType8(): Box<T> = TODO()
fun <T> functionWithUnsubstitutedTypeParametersInReturnType9(): Box<T> = TODO()

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
