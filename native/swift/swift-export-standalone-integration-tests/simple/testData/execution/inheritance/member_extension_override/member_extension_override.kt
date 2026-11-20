// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: member_extension_override.kt

// KT-89699: reverse bridges of member extensions (double-receiver functions) have to be bound by the
// extension receiver too, otherwise overloads that differ only in the receiver can't be told apart.

open class MemberExtensionAnchor

// The minimal shape: a single member extension.
interface SingleMemberExtension {
    fun Int.bump(): Int
}

fun callSingle(value: SingleMemberExtension, x: Int): Int = with(value) { x.bump() }

// The Compose `Density` shape: same name, different extension receivers.
interface Measurer {
    fun Int.measure(): Int
    fun String.measure(): Int
}

fun measureInt(value: Measurer, x: Int): Int = with(value) { x.measure() }
fun measureString(value: Measurer, s: String): Int = with(value) { s.measure() }

// The Compose `Saver` shape: generic interface, extension receiver is another interface.
interface SaverScope {
    fun canBeSaved(value: Any): Boolean
}

class SimpleSaverScope : SaverScope {
    override fun canBeSaved(value: Any): Boolean = true
}

interface Saver<Original, Saveable : Any> {
    fun SaverScope.save(value: Original): Saveable?
    fun restore(value: Saveable): Original?
}

class Payload(val text: String)

fun <O, S : Any> saveWith(saver: Saver<O, S>, value: O): S? = with(saver) { SimpleSaverScope().save(value) }

// Same name and receiver, overloaded on a regular parameter.
interface Combiner {
    fun String.combine(other: Int): String
    fun String.combine(other: String): String
}

fun combineInt(value: Combiner, s: String, x: Int): String = with(value) { s.combine(x) }
fun combineString(value: Combiner, s: String, x: String): String = with(value) { s.combine(x) }

// Open class with overloaded member extensions.
open class OpenMeasurer {
    open fun Int.describe(): String = "kotlin-int"
    open fun String.describe(): String = "kotlin-string"
}

fun describeInt(value: OpenMeasurer, x: Int): String = with(value) { x.describe() }
fun describeString(value: OpenMeasurer, s: String): String = with(value) { s.describe() }
