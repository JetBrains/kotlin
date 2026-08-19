// KIND: STANDALONE
// WITH_PLATFORM_LIBS
// MODULE: ExistentialWrapperViaObjCInterop
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlinx.cinterop.convert
import kotlinx.cinterop.objcPtr
import platform.Foundation.NSMutableArray
import platform.Foundation.NSStringFromClass
import platform.objc.object_getClass

// todo: remove me after KT-87457
interface Anchor {
    fun ping(): Int
}

open class Exported

// Not exported and without an exported ancestor: bridging it to Objective-C makes the runtime build a
// `_KotlinExistential` wrapper.
private class Hidden

// Not exported, but with an exported ancestor, so it resolves to `Exported`'s bound wrapper class instead.
private class HiddenChild : Exported()

// Handing a Kotlin object to Objective-C goes through `Kotlin_Interop_refToObjC`
private fun roundTripThroughObjC(value: Any): Any? =
    NSMutableArray().apply { addObject(value) }.objectAtIndex(0.convert())

private val Any.objCClassName: String
    get() = NSStringFromClass(object_getClass(this)!!)

fun exportedRoundTripsThroughObjC(): Boolean = Exported().let { roundTripThroughObjC(it) === it }

fun hiddenRoundTripsThroughObjC(): Boolean = Hidden().let { roundTripThroughObjC(it) === it }

fun hiddenChildRoundTripsThroughObjC(): Boolean = HiddenChild().let { roundTripThroughObjC(it) === it }

fun unitRoundTripsThroughObjC(): Boolean = roundTripThroughObjC(Unit) === Unit

// Which of the two wrapper resolutions the interop layer used is visible in the wrapper's Objective-C class.
// `classWrapperFor()` hands a non-exported class the bound wrapper class of its closest exported ancestor;
// `protocolWrapperFor()` would hand it an uncached existential instead. So this equality is exactly the
// `conformsTo:` predicate in `+_createRetainedWrapperForKotlinObject:` being always-true rather than `nil`.
fun hiddenChildSharesWrapperClassWithExported(): Boolean =
    HiddenChild().objCClassName == Exported().objCClassName

// The negative control: without it the assertion above would also hold if everything resolved to `Exported`.
// `Hidden` has no exported ancestor, so it is the one case that does legitimately get an existential.
fun hiddenUsesDistinctWrapperClass(): Boolean = Hidden().objCClassName != Exported().objCClassName

// `objcPtr()` is `Kotlin_Interop_refToObjC`, so these observe that a Kotlin object keeps a single wrapper
// across repeated conversions rather than allocating a fresh one each time.
fun existentialWrapperIsCached(): Boolean = Hidden().let { it.objcPtr() == it.objcPtr() }
fun boundWrapperIsCached(): Boolean = HiddenChild().let { it.objcPtr() == it.objcPtr() }
fun exportedWrapperIsCached(): Boolean = Exported().let { it.objcPtr() == it.objcPtr() }

// Entry points for the Swift side, where the argument already carries a bound bridge wrapper of its own.
fun roundTripExportedThroughObjC(value: Exported): Exported = roundTripThroughObjC(value) as Exported

fun makeExported(): Exported = Exported()
