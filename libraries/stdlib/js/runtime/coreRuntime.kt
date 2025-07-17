/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.js.internal.*

@UsedFromCompilerGeneratedCode
internal fun equals(obj1: dynamic, obj2: dynamic): Boolean {
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

@UsedFromCompilerGeneratedCode
internal fun toString(o: dynamic): String = when {
    o == null -> "null"
    isArrayish(o) -> "[...]"
    jsTypeOf(o.toString) != "function" -> anyToString(o)
    else -> (o.toString)().unsafeCast<String>()
}

@UsedFromCompilerGeneratedCode
internal fun anyToString(o: dynamic): String = js("Object").prototype.toString.call(o)

@UsedFromCompilerGeneratedCode
internal fun hashCode(obj: dynamic): Int {
    if (obj == null) return 0

    @Suppress("UNUSED_VARIABLE")
    return when (val typeOf = jsTypeOf(obj)) {
        "object" -> if ("function" === jsTypeOf(obj.hashCode)) (obj.hashCode)() else getObjectHashCode(obj)
        "function" -> getObjectHashCode(obj)
        "number" -> getNumberHashCode(obj)
        "boolean" -> getBooleanHashCode(obj.unsafeCast<Boolean>())
        "string" -> getStringHashCode(js("String")(obj))
        "bigint" -> getBigIntHashCode(obj.unsafeCast<BigInt>())
        "symbol" -> getSymbolHashCode(obj)
        else -> js("throw new Error('Unexpected typeof `' + typeOf + '`')")
    }
}

@UsedFromCompilerGeneratedCode
internal fun getBooleanHashCode(value: Boolean): Int {
    return if (value) 1231 else 1237
}

@UsedFromCompilerGeneratedCode
internal fun getBigIntHashCode(value: BigInt): Int {
    val shiftNumber = BigInt(32)

    // In Kotlin the 0xffffffff literal has the Long type, which is boxed, so we use a Double literal instead
    val mask = BigInt(4294967295.0)

    var bigNumber = value.abs()
    var hashCode = 0
    val signum = if (value.isNegative) -1 else 1

    while (!bigNumber.isZero) {
        val chunk = (bigNumber and mask).toNumber().unsafeCast<Int>()
        hashCode = 31 * hashCode + chunk
        bigNumber = bigNumber shr shiftNumber
    }

    return hashCode * signum
}

@Suppress("MUST_BE_INITIALIZED")
private var symbolWeakMap: dynamic

@Suppress("MUST_BE_INITIALIZED")
private var symbolMap: dynamic

private fun getSymbolWeakMap(): dynamic {
    if (symbolWeakMap === VOID) {
        symbolWeakMap = js("new WeakMap()")
    }
    return symbolWeakMap
}

private fun getSymbolMap(): dynamic {
    if (symbolMap === VOID) {
        symbolMap = js("new Map()")
    }
    return symbolMap
}

@Suppress("UNUSED_PARAMETER")
private fun symbolIsSharable(symbol: dynamic) = js("Symbol.keyFor(symbol)") != VOID

private fun getSymbolHashCode(value: dynamic): Int {
    val hashCodeMap = if (symbolIsSharable(value)) getSymbolMap() else getSymbolWeakMap()
    val cachedHashCode = hashCodeMap.get(value)

    if (cachedHashCode !== VOID) return cachedHashCode

    val hash = calculateRandomHash()
    hashCodeMap.set(value, hash)
    return hash
}

private const val POW_2_32 = 4294967296.0
private const val OBJECT_HASH_CODE_PROPERTY_NAME = "kotlinHashCodeValue$"

private fun calculateRandomHash(): Int {
    return jsBitwiseOr(js("Math").random() * POW_2_32, 0) // Make 32-bit singed integer.
}

@UsedFromCompilerGeneratedCode
internal fun getObjectHashCode(obj: dynamic): Int {
    if (!jsIn(OBJECT_HASH_CODE_PROPERTY_NAME, obj)) {
        var hash = calculateRandomHash()
        var descriptor = js("new Object()")
        descriptor.value = hash
        descriptor.enumerable = false
        js("Object").defineProperty(obj, OBJECT_HASH_CODE_PROPERTY_NAME, descriptor)
    }
    return obj[OBJECT_HASH_CODE_PROPERTY_NAME].unsafeCast<Int>();
}

@UsedFromCompilerGeneratedCode
internal fun getStringHashCode(str: String): Int {
    var hash = 0
    val length: Int = str.length  // TODO: Implement WString.length
    for (i in 0..length-1) {
        val code: Int = str.asDynamic().charCodeAt(i)
        hash = hash * 31 + code
    }
    return hash
}

internal fun identityHashCode(obj: Any?): Int = getObjectHashCode(obj)

@UsedFromCompilerGeneratedCode
internal fun captureStack(instance: Throwable, constructorFunction: Any) {
    if (js("Error").captureStackTrace != null) {
        js("Error").captureStackTrace(instance, constructorFunction)
    } else {
        instance.asDynamic().stack = js("new Error()").stack
    }
}

private fun defineMessage(message: String?, cause: Throwable?): String? =
    if (isUndefined(message)) {
        if (isUndefined(cause)) message else cause?.toString() ?: VOID
    } else message ?: VOID

@UsedFromCompilerGeneratedCode
internal fun newThrowable(message: String?, cause: Throwable?): Throwable {
    val throwable = js("new Error()")
    throwable.message = defineMessage(message, cause)
    throwable.cause = cause
    throwable.name = "Throwable"
    return throwable.unsafeCast<Throwable>()
}

@UsedFromCompilerGeneratedCode
internal fun setupCauseParameter(cause: Throwable?) = js("{ cause: cause }")

@UsedFromCompilerGeneratedCode
internal fun setPropertiesToThrowableInstance(this_: dynamic, message: String?, cause: Throwable?) {
    this_.name = JsObject.getPrototypeOf(this_).constructor.name
    if (message == null) {
        this_.message = if (isUndefined(message)) cause?.toString() ?: VOID else VOID
    }
}

private fun defineFieldOnInstance(this_: dynamic, name: String, value: dynamic) {
    js("Object.defineProperty(this_, name, { configurable: true, writable: true, value: value })")
}

@Suppress("UNUSED") // calls to this function are emitted by the compiler
internal fun extendThrowable(this_: dynamic, message: String?, cause: Throwable?) {
    defineFieldOnInstance(this_, "message", defineMessage(message, cause))
    defineFieldOnInstance(this_, "cause", cause)
    defineFieldOnInstance(this_, "name", JsObject.getPrototypeOf(this_).constructor.name)
}

@JsName("Object")
@UsedFromCompilerGeneratedCode
internal external class JsObject {
    companion object {
        fun getPrototypeOf(obj: Any?): dynamic
        fun setPrototypeOf(obj: Any?, prototype: Any?): dynamic
    }
}

// Note: once some error-compilation design happened consider to distinguish a special exception for error-code.
internal fun errorCode(description: String): Nothing {
    throw IllegalStateException(description)
}

@Suppress("SENSELESS_COMPARISON")
internal fun isUndefined(value: dynamic): Boolean = value === VOID

@UsedFromCompilerGeneratedCode
internal fun <T, R> boxIntrinsic(@Suppress("UNUSED_PARAMETER") x: T): R = error("Should be lowered")

@UsedFromCompilerGeneratedCode
internal fun <T, R> unboxIntrinsic(@Suppress("UNUSED_PARAMETER") x: T): R = error("Should be lowered")

@Suppress("UNUSED_PARAMETER")
@UsedFromCompilerGeneratedCode
internal fun protoOf(constructor: Any) =
    js("constructor.prototype")

@Suppress("UNUSED_PARAMETER")
@UsedFromCompilerGeneratedCode
internal fun <T> objectCreate(proto: T? = null): T =
    js("Object.create(proto)")

@Suppress("UNUSED_PARAMETER")
@UsedFromCompilerGeneratedCode
internal fun createThis(ctor: Ctor, box: dynamic): dynamic {
    val self = js("Object.create(ctor.prototype)")
    boxApply(self, box)
    return self
}

@Suppress("UNUSED_PARAMETER")
@UsedFromCompilerGeneratedCode
internal fun boxApply(self: dynamic, box: dynamic) {
    if (box !== VOID) js("Object.assign(self, box)")
}

@OptIn(JsIntrinsic::class)
@Suppress("UNUSED_PARAMETER", "UNUSED_VARIABLE", "REIFIED_TYPE_PARAMETER_NO_INLINE")
@UsedFromCompilerGeneratedCode
internal fun <reified T : Any> createExternalThis(
    ctor: JsClass<T>,
    superExternalCtor: JsClass<T>,
    parameters: Array<Any?>,
    box: dynamic
): T {
    val selfCtor = if (box === VOID) {
        ctor
    } else {
        val newCtor: dynamic = jsNewAnonymousClass(ctor)
        js("Object.assign(newCtor.prototype, box)")
        newCtor.constructor = ctor
        newCtor
    }
    return js("Reflect.construct(superExternalCtor, parameters, selfCtor)")
}

@Suppress("UNUSED_PARAMETER")
@UsedFromCompilerGeneratedCode
internal fun defineProp(obj: Any, name: String, getter: Any?, setter: Any?, enumerable: Boolean?) =
    js("Object.defineProperty(obj, name, { configurable: true, get: getter, set: setter, enumerable: enumerable })")
