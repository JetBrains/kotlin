// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: closure_signature_override.kt

// Cross-language overriding of members that take or return a closure. A reverse bridge flips every
// conversion of the forward bridge, so a closure parameter travels Kotlin -> Swift and a closure
// return value travels Swift -> Kotlin - the direction opposite to the forward bridge.

open class Transformer {
    open fun apply(input: Int, transform: (Int) -> Int): Int = transform(input) + 1

    // Returned closure: the Swift override produces it, Kotlin invokes it.
    open fun makeAdder(addend: Int): (Int) -> Int = { it + addend }

    // Closure that itself takes a closure: nesting flips direction again.
    open fun applyTwice(input: Int, transform: ((Int) -> Int) -> Int): Int = transform { it * 2 } + input

    open fun describe(name: String, format: (String) -> String): String = format(name)

    open fun runAction(action: () -> Unit): Unit = action()

    open fun applyOptional(input: Int, transform: ((Int) -> Int)?): Int = transform?.invoke(input) ?: -1
}

fun callApply(t: Transformer, input: Int): Int = t.apply(input) { it * 10 }
fun callMakeAdder(t: Transformer, addend: Int, input: Int): Int = t.makeAdder(addend)(input)
fun callApplyTwice(t: Transformer, input: Int): Int = t.applyTwice(input) { f -> f(3) }
fun callDescribe(t: Transformer, name: String): String = t.describe(name) { "[$it]" }

var actionRuns: Int = 0
fun callRunAction(t: Transformer) {
    t.runAction { actionRuns += 1 }
}

fun callApplyOptional(t: Transformer, input: Int, pass: Boolean): Int =
    t.applyOptional(input, if (pass) ({ v: Int -> v + 100 }) else null)

interface Producer {
    fun produce(): () -> String
    fun consume(block: (String) -> Unit)
}

// A base without `Producer` in its Kotlin hierarchy: the Swift conformer below can only be reached
// through reverse bridges. A bare `KotlinBase` conformer cannot be constructed yet, see KT-88251.
open class ProducerBase

fun callProduce(p: Producer): String = p.produce()()
fun callConsume(p: Producer, value: String) = p.consume { received -> lastConsumed = received }
var lastConsumed: String = ""
