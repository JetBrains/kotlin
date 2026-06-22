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
