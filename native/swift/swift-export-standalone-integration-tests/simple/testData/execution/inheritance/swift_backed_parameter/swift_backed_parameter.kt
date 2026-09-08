// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: swift_backed_parameter.kt

// Reverse bridges are normally exercised with a Swift-backed *receiver* and plain Kotlin arguments. Here the
// argument itself is Swift-backed, so the wrapper has to travel Swift -> Kotlin -> reverse bridge -> Swift in
// contravariant position and arrive as the very same Swift instance, rather than as a fresh wrapper built
// around the Kotlin half of it. It also has to stay usable once it gets there: a virtual call on the argument
// from inside the reverse bridge must go back out through that argument's own patched slot.

open class Cargo {
    open fun label(): String = "kotlin-cargo"
}

open class Handler {
    open fun handle(cargo: Cargo): String = "kotlin-handled:${cargo.label()}"
}

// Kotlin receives the argument and passes it on, so it crosses the bridge as a parameter, not as the receiver.
fun callHandle(handler: Handler, cargo: Cargo): String = handler.handle(cargo)

// The argument never came from Swift at all: Kotlin creates it and hands it to the override.
fun callHandleWithKotlinCargo(handler: Handler): String = handler.handle(Cargo())

// The Swift-backed instance is parked on the Kotlin side first, so reaching the override cannot be a plain
// pass-through of a reference Swift supplied in the same call.
class CargoBox(val cargo: Cargo)

fun callHandleFromBox(handler: Handler, box: CargoBox): String = handler.handle(box.cargo)
