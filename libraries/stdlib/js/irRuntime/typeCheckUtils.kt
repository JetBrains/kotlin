/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

@kotlin.internal.DynamicExtension
public fun <T> dynamic.unsafeCast(): T = this

private fun isInterfaceImpl(ctor: dynamic, iface: dynamic): Boolean {
    if (ctor === iface) return true;

    val self = ::isInterfaceImpl
    return js(
        """
    var metadata = ctor.${'$'}metadata${'$'};
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

inline private fun typeOf(obj: dynamic) = js("typeof obj").unsafeCast<String>()

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

public fun isChar(c: Any): Boolean {
    return js("throw Error(\"isChar is not implemented\")").unsafeCast<Boolean>()
}