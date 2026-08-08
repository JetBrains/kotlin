// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: super_and_direct_dispatch.kt

// Non-virtual ("direct dispatch") forward bridges: a Swift subclass of an open Kotlin class must be
// able to call `super.method()` and to inherit non-overridden open methods without infinitely
// recursing through the vtable slot patched with the Swift reverse trampoline.

open class Vehicle {
    open fun describe(): String = "kotlin-vehicle"
    open fun wheels(): Int = 4
}

fun callDescribe(v: Vehicle): String = v.describe()
fun callWheels(v: Vehicle): Int = v.wheels()
