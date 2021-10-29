/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// a package is omitted to get declarations directly under the module
package kotlin.wasm.internal

import kotlin.reflect.*
import kotlin.reflect.wasm.internal.*

internal fun <T : Any> getKClass(typeInfoData: TypeInfoData): KClass<T> =
    KClassImpl(typeInfoData)

internal fun <T : Any> getKClassFromExpression(e: T): KClass<T> =
    when (e) {
        is String -> PrimitiveClasses.stringClass
        is Int -> PrimitiveClasses.intClass
        is Byte -> PrimitiveClasses.byteClass
        is Float -> PrimitiveClasses.floatClass
        is Boolean -> PrimitiveClasses.booleanClass
        is Double -> PrimitiveClasses.doubleClass
        is Long -> PrimitiveClasses.longClass
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
        else -> getKClass(getTypeInfoTypeDataByPtr(e.typeInfo))
    } as KClass<T>

@Suppress("REIFIED_TYPE_PARAMETER_NO_INLINE")
internal inline fun <reified T : Any> wasmGetKClass(): KClass<T> =
    KClassImpl(wasmGetTypeInfoData<T>())

internal fun createKType(classifier: KClassifier, arguments: Array<KTypeProjection>, isMarkedNullable: Boolean): KType =
    KTypeImpl(classifier, arguments.asList(), isMarkedNullable)

internal fun createKTypeParameter(name: String, upperBounds: Array<KType>, variance: String): KTypeParameter {
    val kVariance = when (variance) {
        "in" -> KVariance.IN
        "out" -> KVariance.OUT
        else -> KVariance.INVARIANT
    }
    return KTypeParameterImpl(name, upperBounds.asList(), kVariance, false)
}

internal fun getStarKTypeProjection(): KTypeProjection =
    KTypeProjection.STAR

internal fun createCovariantKTypeProjection(type: KType): KTypeProjection =
    KTypeProjection.covariant(type)

internal fun createInvariantKTypeProjection(type: KType): KTypeProjection =
    KTypeProjection.invariant(type)

internal fun createContravariantKTypeProjection(type: KType): KTypeProjection =
    KTypeProjection.contravariant(type)
