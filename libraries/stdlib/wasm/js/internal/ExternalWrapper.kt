/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNUSED_PARAMETER") // TODO: Remove after bootstrap update

package kotlin.wasm.internal

import kotlin.wasm.internal.reftypes.anyref
import kotlin.wasm.JsBuiltin
import kotlin.wasm.WasmExport
import kotlin.wasm.internal.WasmCharArray

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
            return obj ? 1231 : 1237;
        default:
            return getStringHashCode(String(obj)); 
    }
}
})()"""
)
private external fun externrefHashCode(ref: ExternalInterfaceType): Int

private fun externrefToString(ref: ExternalInterfaceType): String =
    js("String(ref)")

private fun externrefToUByte(ref: ExternalInterfaceType): UByte =
    js("Number(ref)")

private fun externrefToUShort(ref: ExternalInterfaceType): UShort =
    js("Number(ref)")

private fun externrefToUInt(ref: ExternalInterfaceType): UInt =
    js("Number(ref)")

private fun externrefToULong(ref: ExternalInterfaceType): ULong =
    js("BigInt(ref)")

private fun externrefToInt(ref: ExternalInterfaceType): Int =
    js("Number(ref)")

private fun externrefToLong(ref: ExternalInterfaceType): Long =
    js("BigInt(ref)")

private fun externrefToBoolean(ref: ExternalInterfaceType): Boolean =
    js("Boolean(ref)")

private fun externrefToFloat(ref: ExternalInterfaceType): Float =
    js("Number(ref)")

private fun externrefToDouble(ref: ExternalInterfaceType): Double =
    js("Number(ref)")

private fun intToExternref(x: Int): JsNumber =
    js("x")

private fun longToExternref(x: Long): JsBigInt =
    js("x")

private fun booleanToExternref(x: Boolean): JsBoolean =
    js("x")

private fun floatToExternref(x: Float): JsNumber =
    js("x")

private fun doubleToExternref(x: Double): JsNumber =
    js("x")

private fun externrefEquals(lhs: ExternalInterfaceType, rhs: ExternalInterfaceType): Boolean =
    js("lhs === rhs")


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

internal fun externRefToAny(ref: ExternalInterfaceType): Any? {
    // TODO rewrite it so to get something like:
    // block {
    //     refAsAnyref
    //     br_on_cast_fail null 0 $kotlin.Any
    //     return
    // }
    // If ref is an instance of kotlin class -- return it casted to Any
    returnArgumentIfItIsKotlinAny()

    // If we have Null in notNullRef -- return null
    // If we already have a box -- return it,
    // otherwise -- remember new box and return it.
    return getCachedJsObject(ref, JsExternalBox(ref).toJsReference())
}


internal fun anyToExternRef(x: Any): ExternalInterfaceType {
    return if (x is JsExternalBox)
        x.ref
    else
        x.asWasmExternRef()
}

internal fun stringLength(x: ExternalInterfaceType): Int =
    js("x.length")

//@WasmImport("wasm:js-string", "fromCharCodeArray")
@Suppress("WRONG_JS_INTEROP_TYPE")
@JsBuiltin(
    "js-string",
    "fromCharCodeArray",
    """const moduleFromCharCode = new WebAssembly.Module(new Uint8Array([
  0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x0b, 0x02, 0x5e,
  0x77, 0x01, 0x60, 0x02, 0x64, 0x00, 0x7f, 0x01, 0x7f, 0x03, 0x02, 0x01,
  0x01, 0x07, 0x0b, 0x01, 0x07, 0x61, 0x31, 0x36, 0x5f, 0x67, 0x65, 0x74,
  0x00, 0x00, 0x0a, 0x0b, 0x01, 0x09, 0x00, 0x20, 0x00, 0x20, 0x01, 0xfb,
  0x0d, 0x00, 0x0b, 0x00, 0x13, 0x04, 0x6e, 0x61, 0x6d, 0x65, 0x04, 0x0c,
  0x01, 0x00, 0x09, 0x61, 0x72, 0x72, 0x61, 0x79, 0x5f, 0x69, 0x31, 0x36
]));
const helpersFromCharCode = new WebAssembly.Instance(moduleFromCharCode).exports;

export function fromCharCodeArray(array, start, end) {
    start >>>= 0;
    end >>>= 0;
    let result = [];
    for (let i = start; i < end; i++) {
        result.push(String.fromCharCode(helpersFromCharCode.a16_get(array, i)));
    }
    return result.join("");
}
"""
)
internal external fun fromCharCodeArray(array: WasmCharArray, start: Int, end: Int): JsStringRef

internal fun kotlinToJsStringAdapter(x: String?): JsString? {
    // Using nullable String to represent default value
    // for parameters with default values
    if (x == null) return null
    if (x.isEmpty()) return jsEmptyString

    val srcArray = x.chars
    val stringLength = srcArray.len()
    return fromCharCodeArray(srcArray, 0, stringLength)
}

internal fun jsCheckIsNullOrUndefinedAdapter(x: ExternalInterfaceType?): ExternalInterfaceType? =
    // We deliberately avoid usage of `takeIf` here as type erase on the inlining stage leads to infinite recursion
    if (isNullish(x)) null else x

@Suppress("WRONG_JS_INTEROP_TYPE")
@JsBuiltin(
    "js-string",
    "intoCharCodeArray",
    """const moduleIntoCharCode = new WebAssembly.Module(new Uint8Array([
  0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x0b, 0x02, 0x5e,
  0x77, 0x01, 0x60, 0x03, 0x64, 0x00, 0x7f, 0x7f, 0x00, 0x03, 0x02, 0x01,
  0x01, 0x07, 0x0b, 0x01, 0x07, 0x61, 0x31, 0x36, 0x5f, 0x73, 0x65, 0x74,
  0x00, 0x00, 0x0a, 0x0d, 0x01, 0x0b, 0x00, 0x20, 0x00, 0x20, 0x01, 0x20,
  0x02, 0xfb, 0x0e, 0x00, 0x0b, 0x00, 0x13, 0x04, 0x6e, 0x61, 0x6d, 0x65,
  0x04, 0x0c, 0x01, 0x00, 0x09, 0x61, 0x72, 0x72, 0x61, 0x79, 0x5f, 0x69,
  0x31, 0x36
]));
const helpersIntoCharCode = new WebAssembly.Instance(moduleIntoCharCode).exports;

export function intoCharCodeArray(s, array, start) {
    start >>>= 0;
    for (let i = 0; i < s.length; i++) {
      helpersIntoCharCode.a16_set(array, start + i, s.charCodeAt(i));
    }
    return s.length;
}
"""
)
internal external fun intoCharCodeArray(string: ExternalInterfaceType, array: WasmCharArray, start: Int): Int

internal fun jsToKotlinStringAdapter(x: ExternalInterfaceType): String {
    val stringLength = stringLength(x)
    val dstArray = WasmCharArray(stringLength)
    intoCharCodeArray(x, dstArray, 0)
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

internal fun externRefToKotlinUByteAdapter(x: ExternalInterfaceType): UByte =
    externrefToUByte(x)

internal fun externRefToKotlinUShortAdapter(x: ExternalInterfaceType): UShort =
    externrefToUShort(x)

internal fun externRefToKotlinUIntAdapter(x: ExternalInterfaceType): UInt =
    externrefToUInt(x)

internal fun externRefToKotlinULongAdapter(x: ExternalInterfaceType): ULong =
    externrefToULong(x)

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

private fun kotlinUByteToJsNumberUnsafe(x: Int): JsNumber =
    js("x & 0xFF")

private fun kotlinUShortToJsNumberUnsafe(x: Int): JsNumber =
    js("x & 0xFFFF")

private fun kotlinUIntToJsNumberUnsafe(x: Int): JsNumber =
    js("x >>> 0")

private fun kotlinULongToJsBigIntUnsafe(x: Long): JsBigInt =
    js("x & 0xFFFFFFFFFFFFFFFFn")

internal fun kotlinUByteToJsNumber(x: UByte): JsNumber =
    kotlinUByteToJsNumberUnsafe(x.toInt())

internal fun kotlinUShortToJsNumber(x: UShort): JsNumber =
    kotlinUShortToJsNumberUnsafe(x.toInt())

internal fun kotlinUIntToJsNumber(x: UInt): JsNumber =
    kotlinUIntToJsNumberUnsafe(x.toInt())

internal fun kotlinULongToJsBigInt(x: ULong): JsBigInt =
    kotlinULongToJsBigIntUnsafe(x.toLong())

internal fun kotlinLongToExternRefAdapter(x: Long): JsBigInt =
    longToExternref(x)

internal fun kotlinFloatToExternRefAdapter(x: Float): JsNumber =
    floatToExternref(x)

internal fun kotlinDoubleToExternRefAdapter(x: Double): JsNumber =
    doubleToExternref(x)

internal fun kotlinByteToExternRefAdapter(x: Byte): JsNumber =
    intToExternref(x.toInt())

internal fun kotlinShortToExternRefAdapter(x: Short): JsNumber =
    intToExternref(x.toInt())

internal fun kotlinCharToExternRefAdapter(x: Char): JsNumber =
    intToExternref(x.code)

internal fun newJsArray(): ExternalInterfaceType =
    js("[]")

internal fun jsArrayPush(array: ExternalInterfaceType, element: ExternalInterfaceType) {
    js("array.push(element);")
}
