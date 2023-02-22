/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

internal fun setMetadataFor(
    ctor: Ctor,
    name: String?,
    metadataConstructor: (name: String?, associatedObjectKey: Number?, associatedObjects: dynamic, suspendArity: Array<Int>?) -> Metadata,
    parent: Ctor?,
    interfaces: Array<dynamic>?,
    associatedObjectKey: Number?,
    associatedObjects: dynamic,
    suspendArity: Array<Int>?
) {
    if (parent != null) {
        js("""
          ctor.prototype = Object.create(parent.prototype)
          ctor.prototype.constructor = ctor;
        """)
    }

    val metadata = metadataConstructor(name, associatedObjectKey, associatedObjects, suspendArity ?: js("[]"))
    ctor.`$metadata$` = metadata

    if (interfaces != null) {
        val receiver = if (metadata.iid != null) ctor else ctor.prototype
        receiver.`$imask$` = implement(interfaces)
    }
}

// There was a problem with per-module compilation (KT-55758) when the top-level state (iid) was reinitialized during stdlib module initialization
// As a result we miss already incremented iid and had the same iids in two different modules
// So, to keep the state consistent it was moved into the next lateinit variable and function
private lateinit var iid: Any

private fun generateInterfaceId(): Int {
    if (!::iid.isInitialized) {
        iid = 0
    }
    iid = iid.unsafeCast<Int>() + 1
    return iid.unsafeCast<Int>()
}


internal fun interfaceMeta(name: String?, associatedObjectKey: Number?, associatedObjects: dynamic, suspendArity: Array<Int>?): Metadata {
    return createMetadata("interface", name, associatedObjectKey, associatedObjects, suspendArity, generateInterfaceId())
}

internal fun objectMeta(name: String?, associatedObjectKey: Number?, associatedObjects: dynamic, suspendArity: Array<Int>?): Metadata {
    return createMetadata("object", name, associatedObjectKey, associatedObjects, suspendArity, null)
}

internal fun classMeta(name: String?, associatedObjectKey: Number?, associatedObjects: dynamic, suspendArity: Array<Int>?): Metadata {
    return createMetadata("class", name, associatedObjectKey, associatedObjects, suspendArity, null)
}

// Seems like we need to disable this check if variables are used inside js annotation
@Suppress("UNUSED_PARAMETER", "UNUSED_VARIABLE")
private fun createMetadata(
    kind: String,
    name: String?,
    associatedObjectKey: Number?,
    associatedObjects: dynamic,
    suspendArity: Array<Int>?,
    iid: Int?
): Metadata {
    val undef = VOID
    return js("""({
    kind: kind,
    simpleName: name,
    associatedObjectKey: associatedObjectKey,
    associatedObjects: associatedObjects,
    suspendArity: suspendArity,
    ${'$'}kClass$: undef,
    iid: iid
})""")
}

internal external interface Metadata {
    val kind: String
    // This field gives fast access to the prototype of metadata owner (Object.getPrototypeOf())
    // Can be pre-initialized or lazy initialized and then should be immutable
    val simpleName: String?
    val associatedObjectKey: Number?
    val associatedObjects: dynamic
    val suspendArity: Array<Int>?
    val iid: Int?

    var `$kClass$`: dynamic

    var errorInfo: Int? // Bits set for overridden properties: "message" => 0x1, "cause" => 0x2
}

internal external interface Ctor {
    var `$imask$`: BitMask?
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

private fun searchForMetadata(obj: dynamic): Metadata? {
    if (obj == null) {
        return null
    }
    var metadata: Metadata? = obj.`$metadata$`
    var currentObject = getPrototypeOf(obj)

    while (metadata == null && currentObject != null) {
        val currentConstructor = currentObject.constructor
        metadata = currentConstructor.`$metadata$`
        currentObject = getPrototypeOf(currentObject)
    }

    return metadata
}

private fun isInterfaceImpl(obj: dynamic, iface: Int): Boolean {
    val mask: BitMask = obj.`$imask$`.unsafeCast<BitMask?>() ?: return false
    return mask.isBitSet(iface)
}

internal fun isInterface(obj: dynamic, iface: dynamic): Boolean {
    return isInterfaceImpl(obj, iface.`$metadata$`.iid)
}

internal fun isSuspendFunction(obj: dynamic, arity: Int): Boolean {
    if (jsTypeOf(obj) == "function") {
        @Suppress("DEPRECATED_IDENTITY_EQUALS")
        return obj.`$arity`.unsafeCast<Int>() === arity
    }

    if (jsTypeOf(obj) == "object" && jsIn("${'$'}metadata${'$'}", obj.constructor)) {
        @Suppress("IMPLICIT_BOXING_IN_IDENTITY_EQUALS")
        return obj.constructor.unsafeCast<Ctor>().`$metadata$`.suspendArity?.let {
            var result = false
            for (item in it) {
                if (arity == item) {
                    result = true
                    break
                }
            }
            return result
        } ?: false
    }

    return false
}

internal fun isObject(obj: dynamic): Boolean {
    val objTypeOf = jsTypeOf(obj)

    return when (objTypeOf) {
        "string" -> true
        "number" -> true
        "boolean" -> true
        "function" -> true
        else -> jsInstanceOf(obj, js("Object"))
    }
}

private fun isJsArray(obj: Any): Boolean {
    return js("Array").isArray(obj).unsafeCast<Boolean>()
}

internal fun isArray(obj: Any): Boolean {
    return isJsArray(obj) && !(obj.asDynamic().`$type$`)
}

internal fun isArrayish(o: dynamic) = isJsArray(o) || arrayBufferIsView(o)

internal fun isChar(@Suppress("UNUSED_PARAMETER") c: Any): Boolean {
    error("isChar is not implemented")
}

// TODO: Distinguish Boolean/Byte and Short/Char
internal fun isBooleanArray(a: dynamic): Boolean = isJsArray(a) && a.`$type$` === "BooleanArray"
internal fun isByteArray(a: dynamic): Boolean = jsInstanceOf(a, js("Int8Array"))
internal fun isShortArray(a: dynamic): Boolean = jsInstanceOf(a, js("Int16Array"))
internal fun isCharArray(a: dynamic): Boolean = jsInstanceOf(a, js("Uint16Array")) && a.`$type$` === "CharArray"
internal fun isIntArray(a: dynamic): Boolean = jsInstanceOf(a, js("Int32Array"))
internal fun isFloatArray(a: dynamic): Boolean = jsInstanceOf(a, js("Float32Array"))
internal fun isDoubleArray(a: dynamic): Boolean = jsInstanceOf(a, js("Float64Array"))
internal fun isLongArray(a: dynamic): Boolean = isJsArray(a) && a.`$type$` === "LongArray"

internal fun jsGetPrototypeOf(jsClass: dynamic) = js("Object").getPrototypeOf(jsClass)

internal fun jsIsType(obj: dynamic, jsClass: dynamic): Boolean {
    if (jsClass === js("Object")) {
        return isObject(obj)
    }

    if (obj == null || jsClass == null || (jsTypeOf(obj) != "object" && jsTypeOf(obj) != "function")) {
        return false
    }

    if (jsTypeOf(jsClass) == "function" && jsInstanceOf(obj, jsClass)) {
        return true
    }

    var proto = jsGetPrototypeOf(jsClass)
    var constructor = proto?.constructor
    if (constructor != null && jsIn("${'$'}metadata${'$'}", constructor)) {
        var metadata = constructor.`$metadata$`
        if (metadata.kind === "object") {
            return obj === jsClass
        }
    }

    var klassMetadata = jsClass.`$metadata$`

    // In WebKit (JavaScriptCore) for some interfaces from DOM typeof returns "object", nevertheless they can be used in RHS of instanceof
    if (klassMetadata == null) {
        return jsInstanceOf(obj, jsClass)
    }

    if (klassMetadata.kind === "interface") {
        val iid =  klassMetadata.iid.unsafeCast<Int?>() ?: return false
        return isInterfaceImpl(obj, iid)
    }

    return false
}

internal fun isNumber(a: dynamic) = jsTypeOf(a) == "number" || a is Long

@OptIn(JsIntrinsic::class)
internal fun isComparable(value: dynamic): Boolean {
    var type = jsTypeOf(value)

    return type == "string" ||
            type == "boolean" ||
            isNumber(value) ||
            isInterface(value, jsClassIntrinsic<Comparable<*>>())
}

@OptIn(JsIntrinsic::class)
internal fun isCharSequence(value: dynamic): Boolean =
    jsTypeOf(value) == "string" || isInterface(value, jsClassIntrinsic<CharSequence>())


@OptIn(JsIntrinsic::class)
internal fun isExternalObject(value: dynamic, ktExternalObject: dynamic) =
    jsEqeqeq(value, ktExternalObject) || (jsTypeOf(ktExternalObject) == "function" && jsInstanceOf(value, ktExternalObject))