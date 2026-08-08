// KIND: STANDALONE
// MODULE: main(hidden)
// FILE: main.kt
import hidden.HiddenClass
import hidden.HiddenInterface

fun consume_hidden_class(arg: HiddenClass): Unit = TODO()

fun produce_hidden_class(): HiddenClass = TODO()

fun consume_hidden_interface(arg: HiddenInterface): Unit = TODO()

fun consume_hidden_lambda(arg: (HiddenClass) -> Unit): Unit = TODO()

val hidden_val: HiddenClass = TODO()

var hidden_var: HiddenClass
    get() = TODO()
    set(value) = TODO()

// Both overloads collapse to the very same Swift signature, so exactly one of them must survive conflict removal.
fun overloaded(arg: HiddenClass): Unit = TODO()

fun overloaded(arg: HiddenInterface): Unit = TODO()

class Container {
    fun member_consuming_hidden(arg: HiddenClass): Unit = TODO()

    val member_property: HiddenClass = TODO()

    fun untouched_member(arg: Int): Int = arg
}

class WithHiddenCtorParam(val x: HiddenClass)

fun untouched_function(arg: Int): Int = arg

// MODULE: hidden
// HIDE_FROM_SWIFT_EXPORT
// FILE: hidden.kt
package hidden

class HiddenClass

interface HiddenInterface
