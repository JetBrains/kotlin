/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.js.internal

import kotlin.js.JsClass

@JsName("PrimitiveClasses")
internal object PrimitiveClasses {
    @JsName("anyClass")
    val anyClass = PrimitiveKClassImpl(js("Object").unsafeCast<JsClass<Any>>(), "Any", { it is Any })

    @JsName("numberClass")
    val numberClass = PrimitiveKClassImpl(js("Number").unsafeCast<JsClass<Number>>(), "Number", { it is Number })

    @JsName("nothingClass")
    val nothingClass = NothingKClassImpl

    @JsName("booleanClass")
    val booleanClass = PrimitiveKClassImpl(js("Boolean").unsafeCast<JsClass<Boolean>>(), "Boolean", { it is Boolean })

    @JsName("byteClass")
    val byteClass = PrimitiveKClassImpl(js("Number").unsafeCast<JsClass<Byte>>(), "Byte", { it is Byte })

    @JsName("shortClass")
    val shortClass = PrimitiveKClassImpl(js("Number").unsafeCast<JsClass<Short>>(), "Short", { it is Short })

    @JsName("intClass")
    val intClass = PrimitiveKClassImpl(js("Number").unsafeCast<JsClass<Int>>(), "Int", { it is Int })

    @JsName("floatClass")
    val floatClass = PrimitiveKClassImpl(js("Number").unsafeCast<JsClass<Float>>(), "Float", { it is Float })

    @JsName("doubleClass")
    val doubleClass = PrimitiveKClassImpl(js("Number").unsafeCast<JsClass<Double>>(), "Double", { it is Double })

    @JsName("arrayClass")
    val arrayClass = PrimitiveKClassImpl(js("Array").unsafeCast<JsClass<Array<*>>>(), "Array", { it is Array<*> })

    @JsName("stringClass")
    val stringClass = PrimitiveKClassImpl(js("String").unsafeCast<JsClass<String>>(), "String", { it is String })

    @JsName("throwableClass")
    val throwableClass = PrimitiveKClassImpl(js("Error").unsafeCast<JsClass<Throwable>>(), "Throwable", { it is Throwable })

    @JsName("booleanArrayClass")
    val booleanArrayClass = PrimitiveKClassImpl(js("Array").unsafeCast<JsClass<BooleanArray>>(), "BooleanArray", { it is BooleanArray })

    @JsName("charArrayClass")
    val charArrayClass = PrimitiveKClassImpl(js("Uint16Array").unsafeCast<JsClass<CharArray>>(), "CharArray", { it is CharArray })

    @JsName("byteArrayClass")
    val byteArrayClass = PrimitiveKClassImpl(js("Int8Array").unsafeCast<JsClass<ByteArray>>(), "ByteArray", { it is ByteArray })

    @JsName("shortArrayClass")
    val shortArrayClass = PrimitiveKClassImpl(js("Int16Array").unsafeCast<JsClass<ShortArray>>(), "ShortArray", { it is ShortArray })

    @JsName("intArrayClass")
    val intArrayClass = PrimitiveKClassImpl(js("Int32Array").unsafeCast<JsClass<IntArray>>(), "IntArray", { it is IntArray })

    @JsName("longArrayClass")
    val longArrayClass = PrimitiveKClassImpl(js("Array").unsafeCast<JsClass<LongArray>>(), "LongArray", { it is LongArray })

    @JsName("floatArrayClass")
    val floatArrayClass = PrimitiveKClassImpl(js("Float32Array").unsafeCast<JsClass<FloatArray>>(), "FloatArray", { it is FloatArray })

    @JsName("doubleArrayClass")
    val doubleArrayClass = PrimitiveKClassImpl(js("Float64Array").unsafeCast<JsClass<DoubleArray>>(), "DoubleArray", { it is DoubleArray })

    @JsName("functionClass")
    fun functionClass(arity: Int): KClassImpl<Any> {
        return functionClasses.get(arity) ?: run {
            @Suppress("IMPLICIT_BOXING_IN_IDENTITY_EQUALS")
            val result = PrimitiveKClassImpl(js("Function").unsafeCast<JsClass<Any>>(), "Function$arity",
                                             { jsTypeOf(it) === "function" && it.asDynamic().length === arity })
            functionClasses.asDynamic()[arity] = result
            result
        }
    }
}

private val functionClasses = arrayOfNulls<KClassImpl<Any>>(0)