/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.cinterop
import kotlin.native.*
import kotlin.native.internal.GCCritical

data class Pinned<out T : Any> internal constructor(private val stablePtr: COpaquePointer) {

    /**
     * Disposes the handle. It must not be [used][get] after that.
     */
    fun unpin() {
        disposeStablePointer(this.stablePtr)
    }

    /**
     * Returns the underlying pinned object.
     */
    fun get(): T = @Suppress("UNCHECKED_CAST") (derefStablePointer(stablePtr) as T)

}

fun <T : Any> T.pin() = Pinned<T>(createStablePointer(this))

inline fun <T : Any, R> T.usePinned(block: (Pinned<T>) -> R): R {
    val pinned = this.pin()
    return try {
        block(pinned)
    } finally {
        pinned.unpin()
    }
}

fun Pinned<ByteArray>.addressOf(index: Int): CPointer<ByteVar> = this.get().addressOfElement(index)
fun ByteArray.refTo(index: Int): CValuesRef<ByteVar> = this.usingPinned { addressOf(index) }

fun Pinned<String>.addressOf(index: Int): CPointer<COpaque> = this.get().addressOfElement(index)
fun String.refTo(index: Int): CValuesRef<COpaque> = this.usingPinned { addressOf(index) }

fun Pinned<CharArray>.addressOf(index: Int): CPointer<COpaque> = this.get().addressOfElement(index)
fun CharArray.refTo(index: Int): CValuesRef<COpaque> = this.usingPinned { addressOf(index) }

fun Pinned<ShortArray>.addressOf(index: Int): CPointer<ShortVar> = this.get().addressOfElement(index)
fun ShortArray.refTo(index: Int): CValuesRef<ShortVar> = this.usingPinned { addressOf(index) }

fun Pinned<IntArray>.addressOf(index: Int): CPointer<IntVar> = this.get().addressOfElement(index)
fun IntArray.refTo(index: Int): CValuesRef<IntVar> = this.usingPinned { addressOf(index) }

fun Pinned<LongArray>.addressOf(index: Int): CPointer<LongVar> = this.get().addressOfElement(index)
fun LongArray.refTo(index: Int): CValuesRef<LongVar> = this.usingPinned { addressOf(index) }

// TODO: pinning of unsigned arrays involves boxing as they are inline classes wrapping signed arrays.
fun Pinned<UByteArray>.addressOf(index: Int): CPointer<UByteVar> = this.get().addressOfElement(index)
fun UByteArray.refTo(index: Int): CValuesRef<UByteVar> = this.usingPinned { addressOf(index) }

fun Pinned<UShortArray>.addressOf(index: Int): CPointer<UShortVar> = this.get().addressOfElement(index)
fun UShortArray.refTo(index: Int): CValuesRef<UShortVar> = this.usingPinned { addressOf(index) }

fun Pinned<UIntArray>.addressOf(index: Int): CPointer<UIntVar> = this.get().addressOfElement(index)
fun UIntArray.refTo(index: Int): CValuesRef<UIntVar> = this.usingPinned { addressOf(index) }

fun Pinned<ULongArray>.addressOf(index: Int): CPointer<ULongVar> = this.get().addressOfElement(index)
fun ULongArray.refTo(index: Int): CValuesRef<ULongVar> = this.usingPinned { addressOf(index) }

fun Pinned<FloatArray>.addressOf(index: Int): CPointer<FloatVar> = this.get().addressOfElement(index)
fun FloatArray.refTo(index: Int): CValuesRef<FloatVar> = this.usingPinned { addressOf(index) }

fun Pinned<DoubleArray>.addressOf(index: Int): CPointer<DoubleVar> = this.get().addressOfElement(index)
fun DoubleArray.refTo(index: Int): CValuesRef<DoubleVar> = this.usingPinned { addressOf(index) }

private inline fun <T : Any, P : CPointed> T.usingPinned(
        crossinline block: Pinned<T>.() -> CPointer<P>
) = object : CValuesRef<P>() {

    override fun getPointer(scope: AutofreeScope): CPointer<P> {
        val pinned = this@usingPinned.pin()
        scope.defer { pinned.unpin() }
        return pinned.block()
    }
}

@SymbolName("Kotlin_Arrays_getByteArrayAddressOfElement")
@GCCritical
private external fun ByteArray.addressOfElement(index: Int): CPointer<ByteVar>

@SymbolName("Kotlin_Arrays_getStringAddressOfElement")
@GCCritical
private external fun String.addressOfElement(index: Int): CPointer<COpaque>

@SymbolName("Kotlin_Arrays_getCharArrayAddressOfElement")
@GCCritical
private external fun CharArray.addressOfElement(index: Int): CPointer<COpaque>

@SymbolName("Kotlin_Arrays_getShortArrayAddressOfElement")
@GCCritical
private external fun ShortArray.addressOfElement(index: Int): CPointer<ShortVar>

@SymbolName("Kotlin_Arrays_getIntArrayAddressOfElement")
@GCCritical
private external fun IntArray.addressOfElement(index: Int): CPointer<IntVar>

@SymbolName("Kotlin_Arrays_getLongArrayAddressOfElement")
@GCCritical
private external fun LongArray.addressOfElement(index: Int): CPointer<LongVar>

@SymbolName("Kotlin_Arrays_getByteArrayAddressOfElement")
@GCCritical
private external fun UByteArray.addressOfElement(index: Int): CPointer<UByteVar>

@SymbolName("Kotlin_Arrays_getShortArrayAddressOfElement")
@GCCritical
private external fun UShortArray.addressOfElement(index: Int): CPointer<UShortVar>

@SymbolName("Kotlin_Arrays_getIntArrayAddressOfElement")
@GCCritical
private external fun UIntArray.addressOfElement(index: Int): CPointer<UIntVar>

@SymbolName("Kotlin_Arrays_getLongArrayAddressOfElement")
@GCCritical
private external fun ULongArray.addressOfElement(index: Int): CPointer<ULongVar>

@SymbolName("Kotlin_Arrays_getFloatArrayAddressOfElement")
@GCCritical
private external fun FloatArray.addressOfElement(index: Int): CPointer<FloatVar>

@SymbolName("Kotlin_Arrays_getDoubleArrayAddressOfElement")
@GCCritical
private external fun DoubleArray.addressOfElement(index: Int): CPointer<DoubleVar>
