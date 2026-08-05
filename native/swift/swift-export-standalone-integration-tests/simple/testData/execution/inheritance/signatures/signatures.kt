// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: signatures.kt



// --- Overloads (KT-87875) ---

// Overloaded members: each overload's reverse bridge must take over the virtual table slot of that
// exact overload, so Kotlin-side dispatch reaches the matching Swift override. `pick()` is final and
// therefore has no slot at all; `same` overloads are told apart by their parameter types only.
open class Overloads {
    fun pick(): String = "kotlin-final"
    open fun pick(arg1: String): String = "kotlin-pick($arg1)"
    open fun pick(arg1: String, arg2: Int): String = "kotlin-pick($arg1, $arg2)"
    open fun same(arg: String): String = "kotlin-same-string($arg)"
    open fun same(arg: Int): String = "kotlin-same-int($arg)"
}

fun callPick1(o: Overloads, arg1: String): String = o.pick(arg1)
fun callPick2(o: Overloads, arg1: String, arg2: Int): String = o.pick(arg1, arg2)
fun callSameString(o: Overloads, arg: String): String = o.same(arg)
fun callSameInt(o: Overloads, arg: Int): String = o.same(arg)

// Overloads declared in an interface: their reverse bridges land in the interface table instead.
interface OverloadedSpeaker {
    fun say(): String
    fun say(times: Int): String
}

open class OverloadedSpeakerBase : OverloadedSpeaker {
    override fun say(): String = "kotlin-say"
    override fun say(times: Int): String = "kotlin-say($times)"
}

fun callSay(s: OverloadedSpeaker): String = s.say()
fun callSayTimes(s: OverloadedSpeaker, times: Int): String = s.say(times)

// Rich signatures with Kotlin-consumed default arguments, and overload selection across reverse bridges.

data class DataPayload(val text: String, val number: Int)

enum class InheritanceMode {
    kotlinMode,
    swiftMode,
}

open class DefaultSignatureBase {
    open fun format(
        prefix: String,
        payload: DataPayload = DataPayload("default-payload", 7),
        mode: InheritanceMode = InheritanceMode.kotlinMode,
        note: String? = null,
        repeat: Int = 2,
    ): String = "$prefix:${payload.text}:$mode:$note:$repeat"
}

fun callFormatWithKotlinDefaults(value: DefaultSignatureBase, prefix: String): String = value.format(prefix)

fun callFormatExplicitly(
    value: DefaultSignatureBase,
    prefix: String,
    payload: DataPayload,
    mode: InheritanceMode,
    note: String?,
    repeat: Int,
): String = value.format(prefix, payload, mode, note, repeat)

open class OverloadedSignatureBase {
    open fun choose(value: String): String = "kotlin-one:$value"
    open fun choose(value: String, count: Int): String = "kotlin-two:$value:$count"
    open fun choose(value: String, count: Int, mode: InheritanceMode): String = "kotlin-three:$value:$count:$mode"
}

fun callChooseOne(value: OverloadedSignatureBase, text: String): String = value.choose(text)
fun callChooseTwo(value: OverloadedSignatureBase, text: String, count: Int): String = value.choose(text, count)
fun callChooseThree(
    value: OverloadedSignatureBase,
    text: String,
    count: Int,
    mode: InheritanceMode,
): String = value.choose(text, count, mode)
