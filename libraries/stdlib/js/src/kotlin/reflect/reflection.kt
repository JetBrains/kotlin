/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// a package is omitted to get declarations directly under the module

import kotlin.reflect.*
import kotlin.reflect.js.internal.*

@JsName("getKClass")
internal fun <T : Any> getKClass(jClass: Any /* JsClass<T> | Array<JsClass<T>> */): KClass<T> {
    return if (js("Array").isArray(jClass)) {
        getKClassM(jClass.unsafeCast<Array<JsClass<T>>>())
    } else {
        getKClass1(jClass.unsafeCast<JsClass<T>>())
    }
}

@JsName("getKClassM")
internal fun <T : Any> getKClassM(jClasses: Array<JsClass<T>>): KClass<T> = when (jClasses.size) {
    1 -> getKClass1(jClasses[0])
    0 -> NothingKClassImpl.unsafeCast<KClass<T>>()
    else -> ErrorKClass().unsafeCast<KClass<T>>()
}

@JsName("getKClassFromExpression")
internal fun <T : Any> getKClassFromExpression(e: T): KClass<T> =
    when (jsTypeOf(e)) {
        "string" -> PrimitiveClasses.stringClass
        "number" -> if (jsBitwiseOr(e, 0).asDynamic() === e) PrimitiveClasses.intClass else PrimitiveClasses.doubleClass
        "boolean" -> PrimitiveClasses.booleanClass
        "function" -> PrimitiveClasses.functionClass(e.asDynamic().length)
        else -> {
            when {
                e is BooleanArray -> PrimitiveClasses.booleanArrayClass
                e is CharArray -> PrimitiveClasses.charArrayClass
                e is ByteArray -> PrimitiveClasses.byteArrayClass
                e is ShortArray -> PrimitiveClasses.shortArrayClass
                e is IntArray -> PrimitiveClasses.intArrayClass
                e is LongArray -> PrimitiveClasses.longArrayClass
                e is FloatArray -> PrimitiveClasses.floatArrayClass
                e is DoubleArray -> PrimitiveClasses.doubleArrayClass
                e is KClass<*> -> KClass::class
                e is Array<*> -> PrimitiveClasses.arrayClass
                else -> {
                    val constructor = js("Object").getPrototypeOf(e).constructor
                    when {
                        constructor === js("Object") -> PrimitiveClasses.anyClass
                        constructor === js("Error") -> PrimitiveClasses.throwableClass
                        else -> {
                            val jsClass: JsClass<T> = constructor
                            getKClass1(jsClass)
                        }
                    }
                }
            }
        }
    }.unsafeCast<KClass<T>>()

@JsName("getKClass1")
internal fun <T : Any> getKClass1(jClass: JsClass<T>): KClass<T> {
    if (jClass === js("String")) return PrimitiveClasses.stringClass.unsafeCast<KClass<T>>()

    val metadata = jClass.asDynamic().`$metadata$`

    return if (metadata != null) {
        if (metadata.`$kClass$` == null) {
            val kClass = SimpleKClassImpl(jClass)
            metadata.`$kClass$` = kClass
            kClass
        } else {
            metadata.`$kClass$`
        }
    } else {
        SimpleKClassImpl(jClass)
    }
}
