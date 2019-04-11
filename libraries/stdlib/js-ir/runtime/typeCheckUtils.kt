/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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
    //TODO: val ctor = obj.constructor
    val ctor = js("obj.constructor")

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

fun typeOf(obj: dynamic): String = js("typeof obj").unsafeCast<String>()

fun jsTypeOf(obj: Any?): String = js("typeof obj").unsafeCast<String>()

fun instanceOf(obj: dynamic, jsClass_local: dynamic) = js("obj instanceof jsClass_local").unsafeCast<Boolean>()

fun isObject(obj: dynamic): Boolean {
    val objTypeOf = typeOf(obj)

    return when (objTypeOf) {
        "string" -> true
        "number" -> true
        "boolean" -> true
        "function" -> true
        else -> js("obj instanceof Object").unsafeCast<Boolean>()
    }
}

private fun isJsArray(obj: Any): Boolean {
    return js("Array.isArray(obj)").unsafeCast<Boolean>()
}

public fun isArray(obj: Any): Boolean {
    return isJsArray(obj) && js("!obj.\$type\$").unsafeCast<Boolean>()
}

public fun isArrayish(o: dynamic) =
    isJsArray(o) || js("ArrayBuffer.isView(o)").unsafeCast<Boolean>()


public fun isChar(c: Any): Boolean {
    return js("throw Error(\"isChar is not implemented\")").unsafeCast<Boolean>()
}

// TODO: Distinguish Boolean/Byte and Short/Char
public fun isBooleanArray(a: dynamic): Boolean = isJsArray(a) && a.`$type$` === "BooleanArray"
public fun isByteArray(a: dynamic): Boolean = js("a instanceof Int8Array").unsafeCast<Boolean>()
public fun isShortArray(a: dynamic): Boolean = js("a instanceof Int16Array").unsafeCast<Boolean>()
public fun isCharArray(a: dynamic): Boolean = isJsArray(a) && a.`$type$` === "CharArray"
public fun isIntArray(a: dynamic): Boolean = js("a instanceof Int32Array").unsafeCast<Boolean>()
public fun isFloatArray(a: dynamic): Boolean = js("a instanceof Float32Array").unsafeCast<Boolean>()
public fun isDoubleArray(a: dynamic): Boolean = js("a instanceof Float64Array").unsafeCast<Boolean>()
public fun isLongArray(a: dynamic): Boolean = isJsArray(a) && a.`$type$` === "LongArray"


internal fun jsIn(x: String, y: dynamic): Boolean = js("x in y")
internal fun jsGetPrototypeOf(jsClass: dynamic) = js("Object").getPrototypeOf(jsClass)

public fun jsIsType(obj: dynamic, jsClass: dynamic): Boolean {
    if (jsClass === js("Object")) {
        return isObject(obj)
    }

    if (obj == null || jsClass == null || (typeOf(obj) != "object" && typeOf(obj) != "function")) {
        return false
    }

    if (typeOf(jsClass) == "function" && instanceOf(obj, jsClass)) {
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
        return instanceOf(obj, jsClass)
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
