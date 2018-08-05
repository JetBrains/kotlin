/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

private fun isInterfaceImpl(ctor: dynamic, iface: dynamic): Boolean {
    if (ctor === iface) return true;

    val self = ::isInterfaceImpl
    return js(
        """
    var metadata = ctor.${"$"}metadata${"$"};
    if (metadata != null) {
        var interfaces = metadata.interfaces;
        for (var i = 0; i < interfaces.length; i++) {
            if (self_0(interfaces[i], iface)) {
                return true;
            }
        }
    }

    var superPrototype = ctor.prototype != null ? Object.getPrototypeOf(ctor.prototype) : null;
    var superConstructor = superPrototype != null ? superPrototype.constructor : null;
    return superConstructor != null && self_0(superConstructor, iface);
    """
    ).unsafeCast<Boolean>()
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

fun typeOf(obj: dynamic) = js("typeof obj").unsafeCast<String>()

fun instanceOf(obj: dynamic, jsClass: dynamic) = js("obj instanceof jsClass").unsafeCast<Boolean>()

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

public fun isArray(obj: Any): Boolean {
    return js("Array.isArray(obj)").unsafeCast<Boolean>()
}

public fun isArrayish(o: dynamic) =
    isArray(o) || js("ArrayBuffer.isView(o)").unsafeCast<Boolean>()


public fun isChar(c: Any): Boolean {
    return js("throw Error(\"isChar is not implemented\")").unsafeCast<Boolean>()
}

// TODO: Distinguish Boolean/Byte and Short/Char
public fun isBooleanArray(a: dynamic) = js("a instanceof Int8Array")
public fun isByteArray(a: dynamic) = js("a instanceof Int8Array")
public fun isShortArray(a: dynamic) = js("a instanceof Int16Array")
public fun isCharArray(a: dynamic) = js("a instanceof Uint16Array")
public fun isIntArray(a: dynamic) = js("a instanceof Int32Array")
public fun isFloatArray(a: dynamic) = js("a instanceof Float32Array")
public fun isDoubleArray(a: dynamic) = js("a instanceof Float64Array")
public fun isLongArray(a: dynamic) = isArray(a)  // TODO: Implement


internal fun jsIn(x: String, y: dynamic): Boolean = js("x in y")
internal fun jsGetPrototypeOf(jsClass: dynamic) = js("Object.getPrototypeOf(jsClass)")

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