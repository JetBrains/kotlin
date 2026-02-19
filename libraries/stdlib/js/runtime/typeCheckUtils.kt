/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.js.internal.boxedLong.BoxedLongApi

internal external interface Ctor {
    var Symbol: dynamic
    var `$metadata$`: Metadata
    var constructor: Ctor?
    val prototype: dynamic
}

private fun hasProp(proto: dynamic, propName: String): Boolean = proto.hasOwnProperty(propName)

internal fun calculateErrorInfo(proto: dynamic): Int {
    val metadata: Metadata? = proto.constructor?.`$metadata$`

    metadata?.errorInfo?.let { return it } // cached

    var result = 0
    if (hasProp(proto, "message")) result = result or 0x1
    if (hasProp(proto, "cause")) result = result or 0x2

    if (result != 0x3) { //
        val parentProto = getPrototypeOf(proto)
        if (parentProto != js("Error").prototype) {
            result = result or calculateErrorInfo(parentProto)
        }
    }

    if (metadata != null) {
        metadata.errorInfo = result
    }

    return result
}

private fun getPrototypeOf(obj: dynamic) = JsObject.getPrototypeOf(obj)

@UsedFromCompilerGeneratedCode
internal fun isInterface(obj: dynamic, iface: dynamic): Boolean =
    obj[iface.Symbol] === true

@UsedFromCompilerGeneratedCode
internal fun isSuspendFunction(obj: dynamic, arity: Int): Boolean {
    val objTypeOf = jsTypeOf(obj)

    if (objTypeOf == "function") {
        @Suppress("DEPRECATED_IDENTITY_EQUALS")
        return obj.`$arity`.unsafeCast<Int>() === arity
    }

    val suspendArity = obj?.constructor.unsafeCast<Ctor?>()?.`$metadata$`?.suspendArity ?: return false

    @Suppress("IMPLICIT_BOXING_IN_IDENTITY_EQUALS")
    var result = false
    for (item in suspendArity) {
        if (arity == item) {
            result = true
            break
        }
    }
    return result
}

internal fun isJsArray(obj: Any): Boolean {
    return js("Array").isArray(obj).unsafeCast<Boolean>()
}

@UsedFromCompilerGeneratedCode
internal fun isArray(obj: Any): Boolean {
    return isJsArray(obj) && !(obj.asDynamic().`$type$`)
}

internal fun isArrayish(o: dynamic) = isJsArray(o) || arrayBufferIsView(o)

internal fun isChar(@Suppress("UNUSED_PARAMETER") c: Any): Boolean {
    error("isChar is not implemented")
}

// TODO: Distinguish Boolean/Byte and Short/Char
@UsedFromCompilerGeneratedCode
internal fun isBooleanArray(a: dynamic): Boolean = isJsArray(a) && a.`$type$` === "BooleanArray"

@UsedFromCompilerGeneratedCode
internal fun isByteArray(a: dynamic): Boolean = jsInstanceOf(a, js("Int8Array"))

@UsedFromCompilerGeneratedCode
internal fun isShortArray(a: dynamic): Boolean = jsInstanceOf(a, js("Int16Array"))

@UsedFromCompilerGeneratedCode
internal fun isCharArray(a: dynamic): Boolean = jsInstanceOf(a, js("Uint16Array")) && a.`$type$` === "CharArray"

@UsedFromCompilerGeneratedCode
internal fun isIntArray(a: dynamic): Boolean = jsInstanceOf(a, js("Int32Array"))

@UsedFromCompilerGeneratedCode
internal fun isFloatArray(a: dynamic): Boolean = jsInstanceOf(a, js("Float32Array"))

@UsedFromCompilerGeneratedCode
internal fun isDoubleArray(a: dynamic): Boolean = jsInstanceOf(a, js("Float64Array"))

@UsedFromCompilerGeneratedCode
internal fun jsIsFunction(a: dynamic): Boolean = jsTypeOf(a) === "function"

// TODO: Remove after bootstrap update
@BoxedLongApi
@Deprecated("Moved to kotlin.js.internal.boxedLong package", level = DeprecationLevel.HIDDEN)
internal fun isLongArray(a: dynamic): Boolean = kotlin.js.internal.boxedLong.isLongArray(a)

internal fun jsGetPrototypeOf(jsClass: dynamic) = js("Object").getPrototypeOf(jsClass)

internal fun jsIsType(obj: dynamic, jsClass: dynamic): Boolean {
    if (jsClass === js("Object")) {
        return obj != null
    }

    val objType = jsTypeOf(obj)
    val jsClassType = jsTypeOf(jsClass)

    if (obj == null || jsClass == null || (objType != "object" && objType != "function")) {
        return false
    }

    // In WebKit (JavaScriptCore) for some interfaces from DOM typeof returns "object", nevertheless they can be used in RHS of instanceof
    val constructor = if (jsClassType == "object") jsGetPrototypeOf(jsClass) else jsClass
    val klassMetadata = constructor.`$metadata$`

    if (klassMetadata?.kind === METADATA_KIND_INTERFACE) {
        return isInterface(obj, constructor)
    }

    return jsInstanceOf(obj, constructor)
}

@UsedFromCompilerGeneratedCode
internal fun isNumber(a: dynamic) = jsTypeOf(a) == "number" || a is Long

@OptIn(JsIntrinsic::class)
@UsedFromCompilerGeneratedCode
internal fun isComparable(value: dynamic): Boolean {
    val type = jsTypeOf(value)

    return type == "string" ||
            type == "boolean" ||
            isNumber(value) ||
            isInterface(value, jsClassIntrinsic<Comparable<*>>())
}

@OptIn(JsIntrinsic::class)
@UsedFromCompilerGeneratedCode
internal fun isCharSequence(value: dynamic): Boolean =
    jsTypeOf(value) == "string" || isInterface(value, jsClassIntrinsic<CharSequence>())


@OptIn(JsIntrinsic::class)
@UsedFromCompilerGeneratedCode
internal fun isExternalObject(value: dynamic, ktExternalObject: dynamic) =
    jsEqeqeq(value, ktExternalObject) || (jsTypeOf(ktExternalObject) == "function" && jsInstanceOf(value, ktExternalObject))
