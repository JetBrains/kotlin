/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

import kotlin.reflect.KFunction
import kotlin.reflect.wasm.internal.*

internal object PrimitiveClasses {

    val nothingClass = NothingKClassImpl

    val anyClass = wasmGetKClass<Any>()
    val numberClass = wasmGetKClass<Number>()
    val booleanClass = wasmGetKClass<Boolean>()
    val byteClass = wasmGetKClass<Byte>()
    val shortClass = wasmGetKClass<Short>()
    val intClass = wasmGetKClass<Int>()
    val floatClass = wasmGetKClass<Float>()
    val doubleClass = wasmGetKClass<Double>()
    val arrayClass = wasmGetKClass<Array<*>>()
    val stringClass = wasmGetKClass<String>()

    val throwableClass = wasmGetKClass<Throwable>()
    val booleanArrayClass = wasmGetKClass<BooleanArray>()
    val charArrayClass = wasmGetKClass<CharArray>()
    val byteArrayClass = wasmGetKClass<ByteArray>()
    val shortArrayClass = wasmGetKClass<ShortArray>()
    val intArrayClass = wasmGetKClass<IntArray>()
    val longArrayClass = wasmGetKClass<LongArray>()
    val floatArrayClass = wasmGetKClass<FloatArray>()
    val doubleArrayClass = wasmGetKClass<DoubleArray>()

    fun functionClass(arity: Int): KClassImpl<Any> {
        //TODO FunctionN
        return (if (arity == 0) wasmGetKClass<KFunction<*>>() else ErrorKClass) as KClassImpl<Any>
    }
}