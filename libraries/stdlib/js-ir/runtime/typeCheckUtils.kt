/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

private external interface Metadata {
    val interfaces: Array<Ctor>
}

private external interface Ctor {
    val `$metadata$`: Metadata?
    val prototype: Ctor?
}

private fun isInterfaceImpl(ctor: Ctor, iface: dynamic): Boolean {
    if (ctor === iface) return true

    val metadata = ctor.`$metadata$`
    if (metadata != null) {
        val interfaces = metadata.interfaces
        for (i in interfaces) {
            if (isInterfaceImpl(i, iface)) {
                return true
            }
        }
    }

    val superPrototype = if (ctor.prototype != null) js("Object").getPrototypeOf(ctor.prototype) else null
    val superConstructor: Ctor? = if (superPrototype != null) superPrototype.constructor else null
    return superConstructor != null && isInterfaceImpl(superConstructor, iface)
}

public fun isInterface(obj: dynamic, iface: dynamic): Boolean {
    val ctor = obj.constructor

    if (ctor == null) return false

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


fun isObject(obj: dynamic): Boolean {
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

public fun isArray(obj: Any): Boolean {
    return isJsArray(obj) && !(obj.asDynamic().`$type$`)
}

public fun isArrayish(o: dynamic) =
    isJsArray(o) || js("ArrayBuffer").isView(o).unsafeCast<Boolean>()


public fun isChar(c: Any): Boolean {
    error("isChar is not implemented")
}

// TODO: Distinguish Boolean/Byte and Short/Char
public fun isBooleanArray(a: dynamic): Boolean = isJsArray(a) && a.`$type$` === "BooleanArray"
public fun isByteArray(a: dynamic): Boolean = jsInstanceOf(a, js("Int8Array"))
public fun isShortArray(a: dynamic): Boolean = jsInstanceOf(a, js("Int16Array"))
public fun isCharArray(a: dynamic): Boolean = isJsArray(a) && a.`$type$` === "CharArray"
public fun isIntArray(a: dynamic): Boolean = jsInstanceOf(a, js("Int32Array"))
public fun isFloatArray(a: dynamic): Boolean = jsInstanceOf(a, js("Float32Array"))
public fun isDoubleArray(a: dynamic): Boolean = jsInstanceOf(a, js("Float64Array"))
public fun isLongArray(a: dynamic): Boolean = isJsArray(a) && a.`$type$` === "LongArray"


internal fun jsGetPrototypeOf(jsClass: dynamic) = js("Object").getPrototypeOf(jsClass)

public fun jsIsType(obj: dynamic, jsClass: dynamic): Boolean {
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

fun isNumber(a: dynamic) = jsTypeOf(a) == "number" || a is Long

fun isComparable(value: dynamic): Boolean {
    var type = jsTypeOf(value)

    return type == "string" ||
           type == "boolean" ||
           isNumber(value) ||
           isInterface(value, Comparable::class.js)
}

fun isCharSequence(value: dynamic): Boolean =
    jsTypeOf(value) == "string" || isInterface(value, CharSequence::class.js)
