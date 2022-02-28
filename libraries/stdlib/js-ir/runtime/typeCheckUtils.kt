/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

internal fun interfaceMeta(
    name: String?,
    interfaces: Array<Ctor>?,
    associatedObjectKey: Number?,
    associatedObjects: dynamic,
    suspendArity: Array<Int>?,
): Metadata {
    return createMetadata("interface", name, interfaces, associatedObjectKey, associatedObjects, suspendArity, js("undefined"))
}

internal fun objectMeta(
    name: String?,
    interfaces: Array<Ctor>?,
    associatedObjectKey: Number?,
    associatedObjects: dynamic,
    suspendArity: Array<Int>?,
    fastPrototype: Prototype?,
): Metadata {
    return createMetadata("object", name, interfaces, associatedObjectKey, associatedObjects, suspendArity, fastPrototype)
}

internal fun classMeta(
    name: String?,
    interfaces: Array<Ctor>?,
    associatedObjectKey: Number?,
    associatedObjects: dynamic,
    suspendArity: Array<Int>?,
    fastPrototype: Prototype?,
): Metadata {
    return createMetadata("class", name, interfaces, associatedObjectKey, associatedObjects, suspendArity, fastPrototype)
}

// Seems like we need to disable this check if variables are used inside js annotation
@Suppress("UNUSED_PARAMETER")
private fun createMetadata(
    kind: String,
    name: String?,
    interfaces: Array<Ctor>?,
    associatedObjectKey: Number?,
    associatedObjects: dynamic,
    suspendArity: Array<Int>?,
    fastPrototype: Prototype?
): Metadata {
    return js("""({
    kind: kind,
    simpleName: name,
    interfaceId: kind === "interface" ? -1 : undefined,
    interfaces: interfaces || [],
    associatedObjectKey: associatedObjectKey,
    associatedObjects: associatedObjects,
    suspendArity: suspendArity,
    fastPrototype: fastPrototype,
    ${'$'}kClass$: undefined,
    interfacesCache: {
       isComplete: fastPrototype === undefined && (interfaces === undefined || interfaces.length === 0),
       implementInterfaceMemo: {}
    }
})""")
}

internal external interface Metadata {
    val kind: String
    val interfaces: Array<Ctor>
    // This field gives fast access to the prototype of metadata owner (Object.getPrototypeOf())
    // Can be pre-initialized or lazy initialized and then should be immutable
    val simpleName: String?
    val associatedObjectKey: Number?
    val associatedObjects: dynamic
    var interfaceId: Number?
    var fastPrototype: Prototype?
    val suspendArity: Array<Int>?

    var `$kClass$`: dynamic
    // This is an object for memoization of a isInterfaceImpl function
    // Can be mutated quite often
    var interfacesCache: IsImplementsCache?
}

// This is a flag for memoization of a isInterfaceImpl function
internal external interface IsImplementsCache {
    var isComplete: Boolean
    val implementInterfaceMemo: dynamic
}

internal external interface Ctor {
    var `$metadata$`: Metadata?
    var constructor: Ctor?
    val prototype: Prototype?
}

internal external interface Prototype {
    val constructor: Ctor?
}

private var interfacesCounter = 0

private fun Ctor.getOrDefineInterfaceId(): Number? {
    val metadata = `$metadata$`.unsafeCast<Metadata>()
    val interfaceId = metadata.interfaceId ?: -1
    return if (interfaceId != -1) {
        interfaceId
    } else {
        val result = interfacesCounter++
        metadata.interfaceId = result
        result
    }
}

private fun Ctor.getPrototype() = prototype?.let { js("Object").getPrototypeOf(it).unsafeCast<Prototype>() }

internal fun IsImplementsCache.extendCacheWith(cache: IsImplementsCache?) {
    val anotherInterfaceMemo = cache?.implementInterfaceMemo ?: return
    js("Object").assign(implementInterfaceMemo, anotherInterfaceMemo)
}

internal fun IsImplementsCache.extendCacheWithSingle(intr: Ctor) {
    implementInterfaceMemo[intr.getOrDefineInterfaceId()] = true
}

private fun fastGetPrototype(ctor: Ctor): Prototype? {
    return ctor.`$metadata$`?.run {
        if (fastPrototype == null) {
            fastPrototype = ctor.getPrototype()
        }
        fastPrototype
    } ?: ctor.getPrototype()
}

private fun completeInterfaceCache(ctor: Ctor): IsImplementsCache? {
    val metadata = ctor.`$metadata$`

    if (metadata != null && metadata.interfacesCache == null)  {
        metadata.interfacesCache = generateInterfaceCache()
    }

    val interfacesCache = metadata?.interfacesCache

    if (interfacesCache != null) {
        if (interfacesCache.isComplete == true) {
            return interfacesCache
        }

        for (i in metadata.interfaces) {
            interfacesCache.extendCacheWithSingle(i)
            interfacesCache.extendCacheWith(completeInterfaceCache(i))
        }
    }

    val constructor = fastGetPrototype(ctor)?.constructor ?: return null
    val parentInterfacesCache = completeInterfaceCache(constructor) ?: return interfacesCache

    if (interfacesCache == null) return parentInterfacesCache

    interfacesCache.extendCacheWith(parentInterfacesCache)
    interfacesCache.isComplete = true

    return interfacesCache
}

// Old JS Backend
internal fun generateInterfaceCache(): IsImplementsCache {
   return js("{ isComplete: false, implementInterfaceMemo: {} }")
}

private fun isInterfaceImpl(ctor: Ctor, iface: Ctor): Boolean {
    if (ctor === iface) {
        return true
    }

    val metadata = ctor.`$metadata$`

    if (metadata != null && metadata.interfacesCache == null)  {
        metadata.interfacesCache = generateInterfaceCache()
    }

    val interfacesCache = metadata?.interfacesCache

    return if (interfacesCache != null) {
        if (!interfacesCache.isComplete) completeInterfaceCache(ctor)
        val interfaceId = iface.`$metadata$`?.interfaceId ?: return false
        !!interfacesCache.implementInterfaceMemo[interfaceId]
    } else {
        val constructor = fastGetPrototype(ctor)?.constructor ?: return false
        isInterfaceImpl(constructor, iface)
    }
}

internal fun isInterface(obj: dynamic, iface: dynamic): Boolean {
    val ctor = obj.constructor ?: return false
    return isInterfaceImpl(ctor, iface)
}

/*

internal interface ClassMetadata {
    val simpleName: String
    val interfaces: Array<dynamic>
}

// TODO: replace `isInterface` with the following
public fun isInterface(ctor: dynamic, IType: dynamic): Boolean {
    if (ctor === IType) return true

    val metadata = ctor.`$metadata$`.unsafeCast<ClassMetadata?>()

    if (metadata !== null) {
        val interfaces = metadata.interfaces
        for (i in interfaces) {
            if (isInterface(i, IType)) {
                return true
            }
        }
    }

    var superPrototype = ctor.prototype
    if (superPrototype !== null) {
        superPrototype = js("Object.getPrototypeOf(superPrototype)")
    }

    val superConstructor = if (superPrototype !== null) {
        superPrototype.constructor
    } else null

    return superConstructor != null && isInterface(superConstructor, IType)
}
*/

internal fun isSuspendFunction(obj: dynamic, arity: Int): Boolean {
    if (jsTypeOf(obj) == "function") {
        @Suppress("DEPRECATED_IDENTITY_EQUALS")
        return obj.`$arity`.unsafeCast<Int>() === arity
    }

    if (jsTypeOf(obj) == "object" && jsIn("${'$'}metadata${'$'}", obj.constructor)) {
        @Suppress("IMPLICIT_BOXING_IN_IDENTITY_EQUALS")
        return obj.constructor.unsafeCast<Ctor>().`$metadata$`?.suspendArity?.let {
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

    if (klassMetadata.kind === "interface" && obj.constructor != null) {
        return isInterfaceImpl(obj.constructor, jsClass)
    }

    return false
}

internal fun isNumber(a: dynamic) = jsTypeOf(a) == "number" || a is Long

internal fun isComparable(value: dynamic): Boolean {
    var type = jsTypeOf(value)

    return type == "string" ||
            type == "boolean" ||
            isNumber(value) ||
            isInterface(value, Comparable::class.js)
}

internal fun isCharSequence(value: dynamic): Boolean =
    jsTypeOf(value) == "string" || isInterface(value, CharSequence::class.js)
