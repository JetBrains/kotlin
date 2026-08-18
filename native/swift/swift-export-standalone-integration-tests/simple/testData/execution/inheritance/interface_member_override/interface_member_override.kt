// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: interface_member_override.kt

// Swift overrides of members the Kotlin class implements from an interface. The binding targets the class, so
// the vtable slot is patched, and Kotlin-side dispatch typed as the interface must still land on the override.

interface Speaker {
    fun speak(): String
    fun volume(): Int
}

open class SpeakerBase : Speaker {
    override fun speak(): String = "Kotlin speaks"
    override fun volume(): Int = 5
}

fun callSpeak(s: Speaker): String = s.speak()
fun callVolume(s: Speaker): Int = s.volume()

// Two unrelated interfaces on one class: each interface's slot must route independently.

interface Reader { fun read(): String }
interface Writer { fun write(s: String): Int }

open class IoBase : Reader, Writer {
    override fun read(): String = "kotlin reads"
    override fun write(s: String): Int = s.length
}

fun callRead(r: Reader): String = r.read()
fun callWrite(w: Writer, s: String): Int = w.write(s)

// Interface inheritance: dispatch entered through the *parent* interface must reach the same override.

interface Animal { fun name(): String }
interface Dog : Animal { fun bark(): String }

open class DogBase : Dog {
    override fun name(): String = "kotlin-dog"
    override fun bark(): String = "kotlin-woof"
}

fun callName(a: Animal): String = a.name()
fun callBark(d: Dog): String = d.bark()
