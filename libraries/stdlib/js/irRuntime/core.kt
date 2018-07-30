/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

fun equals(obj1: dynamic, obj2: dynamic): Boolean {
    if (js("obj1 == null").unsafeCast<Boolean>()) {
        return js("obj2 == null").unsafeCast<Boolean>();
    }
    if (js("obj2 == null").unsafeCast<Boolean>()) {
        return false;
    }

    return js("""
    if (typeof obj1 === "object" && typeof obj1.equals_Any_ === "function") {
        return obj1.equals_Any_(obj2);
    }

    if (obj1 !== obj1) {
        return obj2 !== obj2;
    }

    if (typeof obj1 === "number" && typeof obj2 === "number") {
        return obj1 === obj2 && (obj1 !== 0 || 1 / obj1 === 1 / obj2)
    }
    return obj1 === obj2;
    """).unsafeCast<Boolean>()
}

fun toString(o: dynamic): String = when {
    js("o == null").unsafeCast<Boolean>() -> "null"
    isArrayish(o) -> "[...]"
    else -> js("o.toString()").unsafeCast<String>()
}

// TODO: Simplify, extract kotlin declarations for inner helper functions
fun hashCode(obj: dynamic): Int {
    return js(
        """
    function hashCode(obj) {
        if (obj == null) {
            return 0;
        }
        var objType = typeof obj;
        if ("object" === objType) {
            return "function" === typeof obj.hashCode ? obj.hashCode() : getObjectHashCode(obj);
        }
        if ("function" === objType) {
            return getObjectHashCode(obj);
        }
        if ("number" === objType) {
            return getNumberHashCode(obj);
        }
        if ("boolean" === objType) {
            return Number(obj)
        }

        var str = String(obj);
        return getStringHashCode(str);
    };

    /** @const */
    var POW_2_32 = 4294967296;
    // TODO: consider switching to Symbol type once we are on ES6.
    /** @const */
    var OBJECT_HASH_CODE_PROPERTY_NAME = "kotlinHashCodeValue${"$"}";

    var byteBuffer = new ArrayBuffer(8);
    var bufFloat64 = new Float64Array(byteBuffer);
    var bufInt32 = new Int32Array(byteBuffer);

    function getObjectHashCode(obj) {
        if (!(OBJECT_HASH_CODE_PROPERTY_NAME in obj)) {
            var hash = (Math.random() * POW_2_32) | 0; // Make 32-bit singed integer.
            Object.defineProperty(obj, OBJECT_HASH_CODE_PROPERTY_NAME, { value:  hash, enumerable: false });
        }
        return obj[OBJECT_HASH_CODE_PROPERTY_NAME];
    }

    function getStringHashCode(str) {
        var hash = 0;
        for (var i = 0; i < str.length; i++) {
            var code  = str.charCodeAt(i);
            hash  = (hash * 31 + code) | 0; // Keep it 32-bit.
        }
        return hash;
    }

    function getNumberHashCode(obj) {
        if ((obj | 0) === obj) {
            return obj | 0;
        }
        else {
            bufFloat64[0] = obj;
            return (bufInt32[1] * 31 | 0) + bufInt32[0] | 0;
        }
    }

    return hashCode(obj);
    """
    ).unsafeCast<Int>()
}

// TODO: Use getObjectHashCode instead
fun identityHashCode(obj: dynamic): Int = hashCode(obj)