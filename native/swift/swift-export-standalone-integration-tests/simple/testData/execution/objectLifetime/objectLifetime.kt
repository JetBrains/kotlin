// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// The no-op GC never collects anything, so released objects stay reachable through the tracking weak reference.
// IGNORE_NATIVE: gcType=NOOP
// MODULE: ObjectLifetime
// FILE: lifetime.kt
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlin.native.runtime.NativeRuntimeApi::class)

import kotlin.native.internal.isPermanent
import kotlin.native.ref.WeakReference
import kotlin.native.runtime.GC

class Bare(val id: Int)

interface Marker

class ExportedMarker : Marker

private class HiddenMarker : Marker

open class Base {
    open fun ping(): Int = 0
}

private var tracked: WeakReference<Any>? = null
private var held: Any? = null

private fun <T : Any> track(value: T): T = value.also { tracked = WeakReference(it) }

fun makeBare(): Bare = track(Bare(1))

fun trackBare(bare: Bare) {
    track(bare)
}

fun makeExportedMarker(): Marker = track(ExportedMarker())

fun makeHiddenMarker(): Marker = track(HiddenMarker())

fun makeAny(): Any = track(Bare(2))

fun holdBare() {
    held = track(Bare(3))
}

fun heldBare(): Bare = held as Bare

fun holdBase(base: Base) {
    held = track(base)
}

fun heldBase(): Base = held as Base

fun callPing(base: Base): Int = base.ping()

fun dropHeld() {
    held = null
}

fun unitAsAny(): Any = Unit

fun isPermanentObject(value: Any): Boolean = value.isPermanent()

fun isTrackedAlive(): Boolean {
    // The rirst one is for the initial cleanup and for running the finalizers,
    // second to cleanup after the previous finalizers,
    // and a third one to ~~rule them all~~ for good measure.
    repeat(3) { GC.collect() }
    return tracked?.get() != null
}
