/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

fun equals(obj1: dynamic, obj2: dynamic): Boolean {
    if (obj1 == null) {
        return obj2 == null
    }
    if (obj2 == null) {
        return false
    }

    return js("""
    if (typeof obj1 === "object" && typeof obj1.equals_kotlin_Any_ === "function") {
        return obj1.equals_kotlin_Any_(obj2);
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

fun anyToString(o: dynamic): String = js("Object.prototype.toString.call(o)")

fun hashCode(obj: dynamic): Int {
    if (obj == null)
        return 0

    return when (typeOf(obj)) {
        "object" ->  if ("function" === js("typeof obj.hashCode")) js("obj.hashCode()") else getObjectHashCode(obj)
        "function" -> getObjectHashCode(obj)
        "number" -> getNumberHashCode(obj)
        "boolean" -> if(obj.unsafeCast<Boolean>()) 1 else 0
        else -> getStringHashCode(js("String(obj)"))
    }
}

fun getObjectHashCode(obj: dynamic) = js("""
    var POW_2_32 = 4294967296;
    var OBJECT_HASH_CODE_PROPERTY_NAME = "kotlinHashCodeValue${"$"}";

    if (!(OBJECT_HASH_CODE_PROPERTY_NAME in obj)) {
        var hash = (Math.random() * POW_2_32) | 0; // Make 32-bit singed integer.
        Object.defineProperty(obj, OBJECT_HASH_CODE_PROPERTY_NAME, { value: hash, enumerable: false });
    }
    return obj[OBJECT_HASH_CODE_PROPERTY_NAME];
""").unsafeCast<Int>();

fun getStringHashCode(str: String): Int {
    var hash = 0
    val length: Int = js("str.length")  // TODO: Implement WString.length
    for (i in 0..length-1) {
        val code: Int = js("str.charCodeAt(i)")
        hash = hash * 31 + code
    }
    return hash
}

fun getNumberHashCode(obj: dynamic) = js("""
    if ((obj | 0) === obj) {
        return obj | 0;
    }
    else {
        var byteBuffer = new ArrayBuffer (8);
        var bufFloat64 = new Float64Array (byteBuffer);
        var bufInt32 = new Int32Array (byteBuffer);

        bufFloat64[0] = obj;
        return (bufInt32[1] * 31 | 0)+bufInt32[0] | 0;
    }
""").unsafeCast<Int>()

// TODO: Use getObjectHashCode instead
fun identityHashCode(obj: dynamic): Int = hashCode(obj)