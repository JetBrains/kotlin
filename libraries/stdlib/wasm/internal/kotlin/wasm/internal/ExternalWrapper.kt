/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

import kotlin.wasm.internal.reftypes.anyref

internal external interface ExternalInterfaceType

internal class JsExternalBox(val ref: ExternalInterfaceType) {
    override fun toString(): String =
        externrefToString(ref)

    override fun equals(other: Any?): Boolean =
        if (other is JsExternalBox) {
            externrefEquals(ref, other.ref)
        } else {
            false
        }

    override fun hashCode(): Int =
        externrefHashCode(ref)
}

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

@JsFun("ref => String(ref)")
private external fun externrefToString(ref: ExternalInterfaceType): String

@JsFun("(lhs, rhs) => lhs === rhs")
private external fun externrefEquals(lhs: ExternalInterfaceType, rhs: ExternalInterfaceType): Boolean

@JsFun("(ref) => (typeof ref !== 'object' || ref === null) ? null : (externrefBoxes.get(ref) ?? null)")
private external fun getExternrefBoxOrNull(ref: ExternalInterfaceType): JsExternalBox?

@JsFun("(ref, box) => { if (typeof ref === 'object' && ref !== null) { externrefBoxes.set(ref, box); } }")
private external fun setExternrefBox(ref: ExternalInterfaceType, box: JsExternalBox)

@WasmNoOpCast
@Suppress("unused")
internal fun Any?.asWasmAnyref(): anyref =
    implementedAsIntrinsic

@WasmNoOpCast
@Suppress("unused")
internal fun ExternalInterfaceType.externAsWasmAnyref(): anyref =
    implementedAsIntrinsic

@WasmNoOpCast
@Suppress("unused")
internal fun Any?.asWasmExternRef(): ExternalInterfaceType =
    implementedAsIntrinsic


@JsFun("(ref) => ref == null")
internal external fun isNullish(ref: ExternalInterfaceType): Boolean

internal fun externRefToAny(ref: ExternalInterfaceType): Any? {
    // If ref is an instance of kotlin class -- return it cased to Any
    val refAsAnyref = ref.externAsWasmAnyref()
    if (wasm_ref_is_data(refAsAnyref)) {
        val refAsDataRef = wasm_ref_as_data(refAsAnyref)
        if (wasm_ref_test<Any>(refAsDataRef)) {
            return wasm_ref_cast<Any>(refAsDataRef)
        }
    }

    if (isNullish(ref))
        return null

    // If we already have a box -- return it,
    // otherwise -- create a new box and remember it.
    return getExternrefBoxOrNull(ref)
        ?: JsExternalBox(ref).also {
            setExternrefBox(ref, it)
        }
}


internal fun anyToExternRef(x: Any): ExternalInterfaceType {
    return if (x is JsExternalBox)
        x.ref
    else
        x.asWasmExternRef()
}

@JsFun("""(addr) => {
    const mem16 = new Uint16Array(wasmExports.memory.buffer);
    const mem32 = new Int32Array(wasmExports.memory.buffer);
    const len = mem32[addr / 4];
    const str_start_addr = (addr + 4) / 2;
    const slice = mem16.slice(str_start_addr, str_start_addr + len);
    return String.fromCharCode.apply(null, slice);
}
""")
internal external fun importStringFromWasm(addr: Int): ExternalInterfaceType

@JsFun("x => x.length")
internal external fun stringLength(x: ExternalInterfaceType): Int

internal fun convertJsStringToKotlinString(x: ExternalInterfaceType): String {
    val length = stringLength(x)
    val addr = unsafeGetScratchRawMemory(INT_SIZE_BYTES + length * CHAR_SIZE_BYTES)
    jsWriteStringIntoMemory(x, addr)
    return String.unsafeFromCharArray(unsafeRawMemoryToWasmCharArray(addr, length))
}


//language=js
@JsFun(
""" (str, addr) => { 
    const memory = new DataView(wasmExports.memory.buffer);
    for (var i = 0; i < str.length; i++) {
        memory.setInt16(addr + i * 2, str.charCodeAt(i), true);
    }
}
"""
)
internal external fun jsWriteStringIntoMemory(str: ExternalInterfaceType, addr: Int)


internal fun kotlinToJsStringAdapter(x: String?): ExternalInterfaceType? {
    // Using nullable String to represent default value
    // for parameters with default values
    if (x == null)
        return null
    return importStringFromWasm(exportString(x))
}

@JsFun("() => true")
internal external fun jsTrue(): ExternalInterfaceType

@JsFun("() => false")
internal external fun jsFalse(): ExternalInterfaceType

internal fun kotlinToJsBooleanAdapter(x: Boolean): ExternalInterfaceType =
    if (x) jsTrue() else jsFalse()

internal fun kotlinToJsAnyAdapter(x: Any): ExternalInterfaceType =
    anyToExternRef(x)

internal fun jsToKotlinAnyAdapter(x: ExternalInterfaceType): Any? =
    externRefToAny(x)

internal fun jsToKotlinStringAdapter(x: ExternalInterfaceType): String =
    convertJsStringToKotlinString(x)

internal fun jsToKotlinByteAdapter(x: Int): Byte = x.toByte()
internal fun jsToKotlinShortAdapter(x: Int): Short = x.toShort()
internal fun jsToKotlinCharAdapter(x: Int): Char = x.toChar()
