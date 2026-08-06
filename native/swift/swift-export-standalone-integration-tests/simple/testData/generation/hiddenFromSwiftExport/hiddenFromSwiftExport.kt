// KIND: STANDALONE
// MODULE: main(hidden)
// FILE: main.kt
import hidden.HiddenClass
import hidden.HiddenInterface
import hidden.HiddenOpenClass

// Inheritance: the whole point of hiding rather than making unavailable. A stub is a real Swift type, so it can
// still be a superclass or a conformance; `Swift.Never` could be neither.
class InheritsHiddenClass : HiddenOpenClass()

class ImplementsHiddenInterface : HiddenInterface

class InheritsAndImplements : HiddenOpenClass(), HiddenInterface

// Plain references to hidden types.
fun consume_hidden_class(arg: HiddenClass): Unit = TODO()

fun produce_hidden_class(): HiddenClass = TODO()

fun consume_hidden_interface(arg: HiddenInterface): Unit = TODO()

val hidden_val: HiddenClass = TODO()

class Container {
    fun member_consuming_hidden(arg: HiddenClass): Unit = TODO()

    val member_property: HiddenClass = TODO()

    fun untouched_member(arg: Int): Int = arg
}

fun untouched_function(arg: Int): Int = arg

// MODULE: hidden
// HIDE_FROM_SWIFT_EXPORT
// FILE: hidden.kt
package hidden

class HiddenClass {
    fun member_that_must_not_be_exported(): Int = TODO()

    val property_that_must_not_be_exported: Int = TODO()
}

open class HiddenOpenClass {
    open fun overridable_member(): Int = TODO()
}

interface HiddenInterface

// A hidden declaration nothing refers to must not show up at all.
class UnreferencedHiddenClass
