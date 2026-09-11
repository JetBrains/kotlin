// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: kotlin_base_direct_inheritance.kt

// KT-88251: a class declared outside of Kotlin may inherit `KotlinBase` directly, with no exported
// Kotlin class anywhere in its hierarchy. Its Kotlin counterpart is a `kotlin.Any`-rooted synthesized
// type, so these tests pin down what such an object can do: satisfy Kotlin interfaces, cross the
// bridge as `Any`, keep its identity, and inherit Kotlin interface defaults.

interface Speaker {
    fun speak(): String
    fun volume(): Int
}

fun callSpeak(s: Speaker): String = s.speak()
fun callVolume(s: Speaker): Int = s.volume()
fun echoSpeaker(s: Speaker): Speaker = s

interface Reader {
    fun read(): String
}

interface Writer {
    fun write(value: String): String
}

fun callRead(r: Reader): String = r.read()
fun callWrite(w: Writer, value: String): String = w.write(value)

// A defaulted member reaching an abstract one: the Swift conformer implements only `tag()`.
interface Defaulter {
    fun tag(): String
    fun describe(): String = "defaulted(${tag()})"
}

fun callDescribe(d: Defaulter): String = d.describe()

// `Any` round-trips, to check that a foreign-rooted object survives the bridge with its identity.
fun echoAny(value: Any): Any = value

class AnyStorage {
    private var stored: Any? = null
    fun store(value: Any) { stored = value }
    fun retrieve(): Any? = stored
}

fun areSame(lhs: Any, rhs: Any): Boolean = lhs === rhs
fun areEqual(lhs: Any, rhs: Any): Boolean = lhs == rhs
fun hashOf(value: Any): Int = value.hashCode()
