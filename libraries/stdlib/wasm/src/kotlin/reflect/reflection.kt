/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// a package is omitted to get declarations directly under the module
package kotlin.wasm.internal

import kotlin.reflect.*
import kotlin.reflect.wasm.internal.*

internal fun <T : Any> getKClass(typeInfoData: TypeInfoData): KClass<T> {
//    return if (js("Array").isArray(jClass)) {
//        getKClassM(jClass.unsafeCast<Array<JsClass<T>>>())
//    } else {
//        getKClass1(jClass.unsafeCast<JsClass<T>>())
//    }

    return getKClass1(typeInfoData)
}

internal fun <T : Any> getKClassM(jClasses: Array<TypeInfoData>): KClass<T> = when (jClasses.size) {
    1 -> getKClass1(jClasses[0])
    0 -> NothingKClassImpl as KClass<T>
    else -> ErrorKClass as KClass<T>
}

internal fun <T : Any> getKClassFromExpression(e: T): KClass<T> =
    when (e) {
        is String -> PrimitiveClasses.stringClass
        is Int -> PrimitiveClasses.intClass
        is Byte -> PrimitiveClasses.byteClass
        is Float -> PrimitiveClasses.floatClass
        is Boolean -> PrimitiveClasses.booleanClass
        is Double -> PrimitiveClasses.doubleClass
        is Number -> PrimitiveClasses.numberClass

        is BooleanArray -> PrimitiveClasses.booleanArrayClass
        is CharArray -> PrimitiveClasses.charArrayClass
        is ByteArray -> PrimitiveClasses.byteArrayClass
        is ShortArray -> PrimitiveClasses.shortArrayClass
        is IntArray -> PrimitiveClasses.intArrayClass
        is LongArray -> PrimitiveClasses.longArrayClass
        is FloatArray -> PrimitiveClasses.floatArrayClass
        is DoubleArray -> PrimitiveClasses.doubleArrayClass
        is KClass<*> -> KClass::class
        is Array<*> -> PrimitiveClasses.arrayClass

        is Function<*> -> PrimitiveClasses.functionClass(0) //TODO
        else -> {
            getKClass1(getTypeInfoTypeDataByPtr(e.typeInfo))
        }
    } as KClass<T>

internal fun <T : Any> getKClass1(infoData: TypeInfoData): KClass<T> {
    return KClassImpl(infoData)
}

@Suppress("REIFIED_TYPE_PARAMETER_NO_INLINE")
internal inline fun <reified T : Any> wasmGetKClass(): KClass<T> =
    KClassImpl(wasmGetTypeInfoData<T>())
