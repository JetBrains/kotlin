// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: inherited_swift_adopted_interface.kt

// KT-88042. A Kotlin interface is adopted by an *intermediate* Swift class, and the object handed to Kotlin is
// an instance of a further Swift subclass that adopts nothing. `class_copyProtocolList` reports a class's own
// adopted protocols only, so the leaf reports none and the interface marker lives on the intermediate class:
// interface discovery has to walk the Swift superclass chain up to the bound Kotlin class, otherwise the
// synthesized TypeInfo carries no interfaces at all and every Kotlin-side check on the leaf fails.
//
// `Base` deliberately declares an open member, so the exported Kotlin class contributes reverse adapters of its
// own and the patched itable is a mix of class- and interface-derived slots. The complementary shape, where the
// Kotlin class declares nothing and the whole itable comes from the interface, is covered by
// `second_swift_level_interface`.

open class Base {
    open fun greet(): String = "Hello from Kotlin"
}

fun callGreet(base: Base): String = base.greet()

interface Speaker {
    fun speak(): String
    fun volume(): Int
}

fun callSpeak(s: Speaker): String = s.speak()
fun callVolume(s: Speaker): Int = s.volume()

// Probe the runtime type check directly, so a failure to discover the conformance is distinguishable from a
// dispatch problem: these fail before the interface list is even consulted for a slot.
fun isSpeaker(a: Any): Boolean = a is Speaker
fun castAndSpeak(a: Any): String = (a as Speaker).speak()

// Two unrelated interfaces adopted at different levels of the Swift chain: discovery must take the *union*
// across the chain, not just the nearest declaring class.

interface Reader { fun read(): String }
interface Writer { fun write(s: String): Int }

fun callRead(r: Reader): String = r.read()
fun callWrite(w: Writer, s: String): Int = w.write(s)

// Interface refinement, to check that the refinement walk still happens when the worklist is seeded from a
// superclass rather than from the leaf.

interface Animal { fun name(): String }
interface Dog : Animal { fun bark(): String }

fun callName(a: Animal): String = a.name()
fun callBark(d: Dog): String = d.bark()

// Property accessors are ordinary itable entries, so an inherited conformance has to route the getter and the
// setter as well.

interface Counter { var count: Int }

fun setCount(c: Counter, n: Int) { c.count = n }
fun getCount(c: Counter): Int = c.count
