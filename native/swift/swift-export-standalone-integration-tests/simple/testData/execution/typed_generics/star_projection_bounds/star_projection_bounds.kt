// KIND: STANDALONE
// MODULE: StarProjectionBounds
// SWIFT_EXPORT_CONFIG: enableGenericsSupport=true
// FILE: StarProjectionBounds.kt

open class Animal(val name: String)
class Dog(name: String) : Animal(name)

// A star projection over a type parameter with an upper bound.
// The Kotlin bridge casts its argument to the type of the parameter: that type must respect the bound.

class NotNull<out C : Any>(val value: C)
class OfAnimal<T : Animal>(var value: T)
class OfNullableAnimal<T : Animal?>(var value: T)
class Two<out A : Any, out B : Animal>(val first: A, val second: B)

fun describeNotNull(x: NotNull<*>): String = "not-null:${x.value}"
fun describeOfAnimal(x: OfAnimal<*>): String = "animal:${x.value.name}"
fun describeOfNullableAnimal(x: OfNullableAnimal<*>): String = "nullable-animal:${x.value?.name}"
fun describeTwo(x: Two<*, Animal>): String = "two:${x.first}/${x.second.name}"
fun describeBothStars(x: Two<*, *>): String = "stars:${x.first}/${x.second.name}"
fun describeOut(x: OfAnimal<out Animal>): String = "out:${x.value.name}"
fun describeOptional(x: NotNull<*>?): String = "optional:${x?.value}"

fun NotNull<*>.describeAsReceiver(): String = "receiver:$value"

class Describer {
    fun describe(x: NotNull<*>): String = "member:${x.value}"
}

// The same types in a return position.
fun starNotNull(): NotNull<*> = NotNull("made in Kotlin")
fun starOfAnimal(): OfAnimal<*> = OfAnimal(Dog("Rex"))
