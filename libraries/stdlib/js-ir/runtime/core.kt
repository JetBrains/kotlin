/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.js.internal.BitUtils

fun equals(obj1: dynamic, obj2: dynamic): Boolean {
    if (obj1 == null) {
        return obj2 == null
    }
    if (obj2 == null) {
        return false
    }

    if (jsTypeOf(obj1) == "object" && jsTypeOf(obj1.equals) == "function") {
        return (obj1.equals)(obj2)
    }

    if (obj1 !== obj1) {
        return obj2 !== obj2
    }

    if (jsTypeOf(obj1) == "number" && jsTypeOf(obj2) == "number") {
        return obj1 === obj2 && (obj1 !== 0 || 1.asDynamic() / obj1 === 1.asDynamic() / obj2)
    }
    return obj1 === obj2
}

fun toString(o: dynamic): String = when {
    o == null -> "null"
    isArrayish(o) -> "[...]"

    else -> (o.toString)().unsafeCast<String>()
}

fun anyToString(o: dynamic): String = js("Object").prototype.toString.call(o)

fun hashCode(obj: dynamic): Int {
    if (obj == null)
        return 0

    return when (jsTypeOf(obj)) {
        "object" -> if ("function" === jsTypeOf(obj.hashCode)) (obj.hashCode)() else getObjectHashCode(obj)
        "function" -> getObjectHashCode(obj)
        "number" -> BitUtils.getNumberHashCode(obj)
        "boolean" -> if(obj.unsafeCast<Boolean>()) 1 else 0
        else -> getStringHashCode(js("String(obj)"))
    }
}

private var POW_2_32 = 4294967296
private var OBJECT_HASH_CODE_PROPERTY_NAME = "kotlinHashCodeValue$"

fun getObjectHashCode(obj: dynamic): Int {
    if (!jsIn(OBJECT_HASH_CODE_PROPERTY_NAME, obj)) {
        var hash = jsBitwiseOr(js("Math").random() * POW_2_32, 0) // Make 32-bit singed integer.
        var descriptor = js("new Object()")
        descriptor.value = hash
        descriptor.enumerable = false
        js("Object").defineProperty(obj, OBJECT_HASH_CODE_PROPERTY_NAME, descriptor)
    }
    return obj[OBJECT_HASH_CODE_PROPERTY_NAME].unsafeCast<Int>();
}

fun getStringHashCode(str: String): Int {
    var hash = 0
    val length: Int = str.length  // TODO: Implement WString.length
    for (i in 0..length-1) {
        val code: Int = str.asDynamic().charCodeAt(i)
        hash = hash * 31 + code
    }
    return hash
}

fun identityHashCode(obj: Any?): Int = getObjectHashCode(obj)

@JsName("captureStack")
internal fun captureStack(instance: Throwable) {
    if (js("Error").captureStackTrace) {
        js("Error").captureStackTrace(instance, instance::class.js);
    } else {
        instance.asDynamic().stack = js("new Error()").stack;
    }
}

@JsName("newThrowable")
internal fun newThrowable(message: String?, cause: Throwable?): Throwable {
    val throwable = js("new Error()")
    throwable.message = if (message == null) {
        if (cause != null) (cause.asDynamic().toString)() else null
    } else {
        message
    }
    throwable.cause = cause
    throwable.name = "Throwable"
    return throwable
}

internal fun <T, R> boxIntrinsic(x: T): R = error("Should be lowered")
internal fun <T, R> unboxIntrinsic(x: T): R = error("Should be lowered")
