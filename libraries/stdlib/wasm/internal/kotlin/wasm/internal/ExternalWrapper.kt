/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNUSED_PARAMETER") // TODO: Remove after bootstrap update

package kotlin.wasm.internal

import kotlin.wasm.internal.reftypes.anyref
import kotlin.wasm.unsafe.withScopedMemoryAllocator
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi

internal typealias ExternalInterfaceType = JsAny

internal class JsExternalBox @WasmPrimitiveConstructor constructor(val ref: ExternalInterfaceType) {
    override fun toString(): String =
        externrefToString(ref)

    override fun equals(other: Any?): Boolean =
        if (other is JsExternalBox) {
            externrefEquals(ref, other.ref)
        } else {
            false
        }

    override fun hashCode(): Int {
        var hashCode = _hashCode
        if (hashCode != 0) return hashCode
        hashCode = externrefHashCode(ref)
        _hashCode = hashCode
        return hashCode
    }
}

@Suppress("DEPRECATION")
//language=js
@JsFun("""
(() => {
const dataView = new DataView(new ArrayBuffer(8));
function numberHashCode(obj) {
    if ((obj | 0) === obj) {
        return obj | 0;
    } else {
        dataView.setFloat64(0, obj, true);
        return (dataView.getInt32(0, true) * 31 | 0) + dataView.getInt32(4, true) | 0;
    }
}

const hashCodes = new WeakMap();
function getObjectHashCode(obj) {
    const res = hashCodes.get(obj);
    if (res === undefined) {
        const POW_2_32 = 4294967296;
        const hash = (Math.random() * POW_2_32) | 0;
        hashCodes.set(obj, hash);
        return hash;
    }
    return res;
}

function getStringHashCode(str) {
    var hash = 0;
    for (var i = 0; i < str.length; i++) {
        var code  = str.charCodeAt(i);
        hash  = (hash * 31 + code) | 0;
    }
    return hash;
}

return (obj) => {
    if (obj == null) {
        return 0;
    }
    switch (typeof obj) {
        case "object":
        case "function":
            return getObjectHashCode(obj);
        case "number":
            return numberHashCode(obj);
        case "boolean":
            return obj;
        default:
            return getStringHashCode(String(obj)); 
    }
}
})()"""
)
private external fun externrefHashCode(ref: ExternalInterfaceType): Int

private fun externrefToString(ref: ExternalInterfaceType): String =
    js("String(ref)")

private fun externrefToInt(ref: ExternalInterfaceType): Int =
    js("Number(ref)")

private fun externrefToLong(ref: ExternalInterfaceType): Long =
    js("Number(ref)")

private fun externrefToBoolean(ref: ExternalInterfaceType): Boolean =
    js("Boolean(ref)")

private fun externrefToFloat(ref: ExternalInterfaceType): Float =
    js("Number(ref)")

private fun externrefToDouble(ref: ExternalInterfaceType): Double =
    js("Number(ref)")

private fun intToExternref(x: Int): JsNumber =
    js("x")

private fun longToExternref(x: Long): ExternalInterfaceType =
    js("x")

private fun booleanToExternref(x: Boolean): ExternalInterfaceType =
    js("x")

private fun floatToExternref(x: Float): ExternalInterfaceType =
    js("x")

private fun doubleToExternref(x: Double): JsNumber =
    js("x")

private fun externrefEquals(lhs: ExternalInterfaceType, rhs: ExternalInterfaceType): Boolean =
    js("lhs === rhs")

private external fun tryGetOrSetExternrefBox(
    ref: ExternalInterfaceType,
    ifNotCached: JsHandle<JsExternalBox>
): JsHandle<JsExternalBox>?

@WasmNoOpCast
@Suppress("unused")
private fun Any?.asWasmAnyref(): anyref =
    implementedAsIntrinsic

@WasmOp(WasmOp.EXTERN_INTERNALIZE)
private fun ExternalInterfaceType.externAsWasmAnyref(): anyref =
    implementedAsIntrinsic

@WasmOp(WasmOp.EXTERN_EXTERNALIZE)
private fun Any.asWasmExternRef(): ExternalInterfaceType =
    implementedAsIntrinsic

internal fun isNullish(ref: ExternalInterfaceType?): Boolean =
    js("ref == null")

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
/*
* Compiler generates inplace next code:
* ```
* block (result anyref) {
*     local.get 0
*     extern.internalize
*     br_on_non_data_fail 0
*     br_on_cast_fail 0 (type $kotlin.Any)
*     return
* }
* ```
*/
internal fun returnArgumentIfItIsKotlinAny(ref: ExternalInterfaceType): Unit = implementedAsIntrinsic

internal fun externRefToAny(ref: ExternalInterfaceType): Any? {
    // TODO rewrite it so to get something like:
    // block {
    //     refAsAnyref
    //     br_on_cast_fail null 0 $kotlin.Any
    //     return
    // }
    // If ref is an instance of kotlin class -- return it casted to Any
    returnArgumentIfItIsKotlinAny(ref)

    // If we have Null in notNullRef -- return null
    // If we already have a box -- return it,
    // otherwise -- remember new box and return it.
    return tryGetOrSetExternrefBox(ref, JsExternalBox(ref).toJsHandle())
}


internal fun anyToExternRef(x: Any): ExternalInterfaceType {
    return if (x is JsExternalBox)
        x.ref
    else
        x.asWasmExternRef()
}

internal fun stringLength(x: ExternalInterfaceType): Int =
    js("x.length")

// kotlin string to js string export
// TODO Uint16Array may work with byte endian different with Wasm (i.e. little endian)
internal fun importStringFromWasm(address: Int, length: Int, prefix: ExternalInterfaceType?): JsString {
    js("""
    const mem16 = new Uint16Array(wasmExports.memory.buffer, address, length);
    const str = String.fromCharCode.apply(null, mem16);
    return (prefix == null) ? str : prefix + str;
    """)
}

internal fun kotlinToJsStringAdapter(x: String?): JsString? {
    // Using nullable String to represent default value
    // for parameters with default values
    if (x == null) return null
    if (x.isEmpty()) return jsEmptyString

    val srcArray = x.chars
    val stringLength = srcArray.len()
    val maxStringLength = STRING_INTEROP_MEM_BUFFER_SIZE / CHAR_SIZE_BYTES

    @OptIn(UnsafeWasmMemoryApi::class)
    withScopedMemoryAllocator { allocator ->
        val memBuffer = allocator.allocate(stringLength.coerceAtMost(maxStringLength) * CHAR_SIZE_BYTES).address.toInt()

        var result: ExternalInterfaceType? = null
        var srcStartIndex = 0
        while (srcStartIndex < stringLength - maxStringLength) {
            unsafeWasmCharArrayToRawMemory(srcArray, srcStartIndex, maxStringLength, memBuffer)
            result = importStringFromWasm(memBuffer, maxStringLength, result)
            srcStartIndex += maxStringLength
        }

        unsafeWasmCharArrayToRawMemory(srcArray, srcStartIndex, stringLength - srcStartIndex, memBuffer)
        return importStringFromWasm(memBuffer, stringLength - srcStartIndex, result)
    }
}

internal fun jsCheckIsNullOrUndefinedAdapter(x: ExternalInterfaceType?): ExternalInterfaceType? =
    x.takeIf { !isNullish(it) }

// js string to kotlin string import
// TODO Uint16Array may work with byte endian different with Wasm (i.e. little endian)
internal fun jsExportStringToWasm(src: ExternalInterfaceType, srcOffset: Int, srcLength: Int, dstAddr: Int) {
    js("""
    const mem16 = new Uint16Array(wasmExports.memory.buffer, dstAddr, srcLength);
    let arrayIndex = 0;
    let srcIndex = srcOffset;
    while (arrayIndex < srcLength) {
        mem16.set([src.charCodeAt(srcIndex)], arrayIndex);
        srcIndex++;
        arrayIndex++;
    }     
    """)
}

private const val STRING_INTEROP_MEM_BUFFER_SIZE = 65_536 // 1 page 4KiB

internal fun jsToKotlinStringAdapter(x: ExternalInterfaceType): String {
    val stringLength = stringLength(x)
    val dstArray = WasmCharArray(stringLength)
    if (stringLength == 0) {
        return dstArray.createString()
    }

    @OptIn(UnsafeWasmMemoryApi::class)
    withScopedMemoryAllocator { allocator ->
        val maxStringLength = STRING_INTEROP_MEM_BUFFER_SIZE / CHAR_SIZE_BYTES
        val memBuffer = allocator.allocate(stringLength.coerceAtMost(maxStringLength) * CHAR_SIZE_BYTES).address.toInt()

        var srcStartIndex = 0
        while (srcStartIndex < stringLength - maxStringLength) {
            jsExportStringToWasm(x, srcStartIndex, maxStringLength, memBuffer)
            unsafeRawMemoryToWasmCharArray(memBuffer, srcStartIndex, maxStringLength, dstArray)
            srcStartIndex += maxStringLength
        }

        jsExportStringToWasm(x, srcStartIndex, stringLength - srcStartIndex, memBuffer)
        unsafeRawMemoryToWasmCharArray(memBuffer, srcStartIndex, stringLength - srcStartIndex, dstArray)
    }

    return dstArray.createString()
}


private fun getJsEmptyString(): JsString =
    js("''")

private fun getJsTrue(): JsBoolean =
    js("true")

private fun getJsFalse(): JsBoolean =
    js("false")

private var _jsEmptyString: JsString? = null
private val jsEmptyString: JsString
    get() {
        var value = _jsEmptyString
        if (value == null) {
            value = getJsEmptyString()
            _jsEmptyString = value
        }

        return value
    }

private var _jsTrue: JsBoolean? = null
private val jsTrue: JsBoolean
    get() {
        var value = _jsTrue
        if (value == null) {
            value = getJsTrue()
            _jsTrue = value
        }

        return value
    }

private var _jsFalse: JsBoolean? = null
private val jsFalse: JsBoolean
    get() {
        var value = _jsFalse
        if (value == null) {
            value = getJsFalse()
            _jsFalse = value
        }

        return value
    }

internal fun numberToDoubleAdapter(x: Number): Double =
    x.toDouble()

internal fun kotlinToJsAnyAdapter(x: Any?): ExternalInterfaceType? =
    if (x == null) null else anyToExternRef(x)

internal fun jsToKotlinAnyAdapter(x: ExternalInterfaceType?): Any? =
    if (x == null) null else externRefToAny(x)

internal fun jsToKotlinByteAdapter(x: Int): Byte = x.toByte()
internal fun jsToKotlinShortAdapter(x: Int): Short = x.toShort()
internal fun jsToKotlinCharAdapter(x: Int): Char = x.toChar()

internal fun externRefToKotlinIntAdapter(x: ExternalInterfaceType): Int =
    externrefToInt(x)

internal fun externRefToKotlinBooleanAdapter(x: ExternalInterfaceType): Boolean =
    externrefToBoolean(x)

internal fun externRefToKotlinLongAdapter(x: ExternalInterfaceType): Long =
    externrefToLong(x)

internal fun externRefToKotlinFloatAdapter(x: ExternalInterfaceType): Float =
    externrefToFloat(x)

internal fun externRefToKotlinDoubleAdapter(x: ExternalInterfaceType): Double =
    externrefToDouble(x)

internal fun kotlinIntToExternRefAdapter(x: Int): JsNumber =
    intToExternref(x)

internal fun kotlinBooleanToExternRefAdapter(x: Boolean): JsBoolean =
    if (x) jsTrue else jsFalse

internal fun kotlinLongToExternRefAdapter(x: Long): ExternalInterfaceType =
    longToExternref(x)

internal fun kotlinFloatToExternRefAdapter(x: Float): ExternalInterfaceType =
    floatToExternref(x)

internal fun kotlinDoubleToExternRefAdapter(x: Double): JsNumber =
    doubleToExternref(x)

internal fun kotlinByteToExternRefAdapter(x: Byte): ExternalInterfaceType =
    intToExternref(x.toInt())

internal fun kotlinShortToExternRefAdapter(x: Short): ExternalInterfaceType =
    intToExternref(x.toInt())

internal fun kotlinCharToExternRefAdapter(x: Char): ExternalInterfaceType =
    intToExternref(x.toInt())

internal fun newJsArray(): ExternalInterfaceType =
    js("[]")

internal fun jsArrayPush(array: ExternalInterfaceType, element: ExternalInterfaceType) {
    js("array.push(element);")
}
