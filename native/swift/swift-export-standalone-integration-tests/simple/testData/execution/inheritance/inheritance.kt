// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: lib.kt

open class Base {
    open fun greet(): String = "Hello from Kotlin"
}

fun callGreet(base: Base): String = base.greet()

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

interface Reader { fun read(): String }
interface Writer { fun write(s: String): Int }

open class IoBase : Reader, Writer {
    override fun read(): String = "kotlin reads"
    override fun write(s: String): Int = s.length
}

fun callRead(r: Reader): String = r.read()
fun callWrite(w: Writer, s: String): Int = w.write(s)

interface Animal { fun name(): String }
interface Dog : Animal { fun bark(): String }

open class DogBase : Dog {
    override fun name(): String = "kotlin-dog"
    override fun bark(): String = "kotlin-woof"
}

fun callName(a: Animal): String = a.name()
fun callBark(d: Dog): String = d.bark()

interface Counter { var count: Int }

open class CounterBase : Counter { override var count: Int = 0 }

fun setCount(c: Counter, n: Int) { c.count = n }
fun getCount(c: Counter): Int = c.count

// Non-virtual ("direct dispatch") forward bridges: a Swift subclass of an open Kotlin class must be
// able to call `super.method()` and to inherit non-overridden open methods without infinitely
// recursing through the vtable slot patched with the Swift reverse trampoline.
open class Vehicle {
    open fun describe(): String = "kotlin-vehicle"
    open fun wheels(): Int = 4
}

fun callDescribe(v: Vehicle): String = v.describe()
fun callWheels(v: Vehicle): Int = v.wheels()

// Defaulted interface methods (methods-only). A Swift class that inherits a Kotlin class and
// first-adopts this interface, without overriding `describe`, must inherit the Kotlin default —
// dispatched non-virtually so it never recurses through its patched itable slot. The default's open
// (virtual) self-call to the abstract `tag()` must still reach the Swift override.
interface Defaulter {
    fun tag(): String
    fun describe(): String = "default-describe(" + tag() + ")"
}

fun callDefDescribe(d: Defaulter): String = d.describe()
fun callDefTag(d: Defaulter): String = d.tag()

// --- Properties ---

// Open class with a get-only (`val`) and a settable (`var`) property, both overridable from Swift.
// Kotlin-side access (readLabel/readNick/writeNick) must reach the Swift accessors via the patched vtable.
open class Named {
    open val label: String = "kotlin-label"
    open var nick: String = "kotlin-nick"
}

fun readLabel(n: Named): String = n.label
fun readNick(n: Named): String = n.nick
fun writeNick(n: Named, v: String) { n.nick = v }

// Property analog of `Vehicle`: a Swift override reading `super.title` must reach the inherited Kotlin
// getter via the non-virtual `_direct` bridge (no recursion); a non-overridden `rank` reached from
// Kotlin must also route through the direct bridge instead of looping through its patched slot.
open class Book {
    open val title: String = "kotlin-title"
    open val rank: Int = 1
}

fun readTitle(b: Book): String = b.title
fun readRank(b: Book): Int = b.rank

// Defaulted interface property: a Swift conformer inherits the Kotlin default getter (`display`)
// without reimplementing it — dispatched non-virtually so it never recurses through the patched
// itable. The default's open self-call to the abstract `base` must reach the Swift override.
interface Labeled {
    val base: String
    val display: String get() = "display(" + base + ")"
}

fun readDisplay(l: Labeled): String = l.display
fun readBase(l: Labeled): String = l.base
