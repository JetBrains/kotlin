/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.js.internal

import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.js.internal.BigInt

@JsName("PrimitiveClasses")
@UsedFromCompilerGeneratedCode
@OptIn(JsIntrinsic::class)
internal object PrimitiveClasses {
    @JsName("anyClass")
    val anyClass = PrimitiveKClassImpl(jsClassIntrinsic<Any>(), "Any", { it is Any })

    @JsName("numberClass")
    val numberClass = PrimitiveKClassImpl(jsClassIntrinsic<Number>(), "Number", { it is Number })

    @JsName("nothingClass")
    val nothingClass = NothingKClassImpl

    @JsName("booleanClass")
    val booleanClass = PrimitiveKClassImpl(jsClassIntrinsic<Boolean>(), "Boolean", { it is Boolean })

    @JsName("byteClass")
    val byteClass = PrimitiveKClassImpl(jsClassIntrinsic<Byte>(), "Byte", { it is Byte })

    @JsName("shortClass")
    val shortClass = PrimitiveKClassImpl(jsClassIntrinsic<Short>(), "Short", { it is Short })

    @JsName("intClass")
    val intClass = PrimitiveKClassImpl(jsClassIntrinsic<Int>(), "Int", { it is Int })

    @JsName("longClass")
    val longClass = PrimitiveKClassImpl(jsClassIntrinsic<Long>(), "Long", { it is Long })

    @JsName("floatClass")
    val floatClass = PrimitiveKClassImpl(jsClassIntrinsic<Float>(), "Float", { it is Float })

    @JsName("doubleClass")
    val doubleClass = PrimitiveKClassImpl(jsClassIntrinsic<Double>(), "Double", { it is Double })

    @JsName("arrayClass")
    val arrayClass = PrimitiveKClassImpl(jsClassIntrinsic<Array<*>>(), "Array", { it is Array<*> })

    @JsName("stringClass")
    val stringClass = PrimitiveKClassImpl(jsClassIntrinsic<String>(), "String", { it is String })

    @JsName("throwableClass")
    val throwableClass = PrimitiveKClassImpl(jsClassIntrinsic<Throwable>(), "Throwable", { it is Throwable })

    @JsName("booleanArrayClass")
    val booleanArrayClass = PrimitiveKClassImpl(jsClassIntrinsic<BooleanArray>(), "BooleanArray", { it is BooleanArray })

    @JsName("charArrayClass")
    val charArrayClass = PrimitiveKClassImpl(jsClassIntrinsic<CharArray>(), "CharArray", { it is CharArray })

    @JsName("byteArrayClass")
    val byteArrayClass = PrimitiveKClassImpl(jsClassIntrinsic<ByteArray>(), "ByteArray", { it is ByteArray })

    @JsName("shortArrayClass")
    val shortArrayClass = PrimitiveKClassImpl(jsClassIntrinsic<ShortArray>(), "ShortArray", { it is ShortArray })

    @JsName("intArrayClass")
    val intArrayClass = PrimitiveKClassImpl(jsClassIntrinsic<IntArray>(), "IntArray", { it is IntArray })

    @JsName("bigIntClass")
    val bigintClass = PrimitiveKClassImpl(
        (if (jsTypeOf(BigInt) == "undefined") VOID else BigInt).unsafeCast<JsClass<BigInt>>(),
        "BigInt",
        { jsTypeOf(it) === "bigint" }
    )

    @JsName("longArrayClass")
    val longArrayClass = PrimitiveKClassImpl(jsClassIntrinsic<LongArray>(), "LongArray", { it is LongArray })

    @JsName("floatArrayClass")
    val floatArrayClass = PrimitiveKClassImpl(jsClassIntrinsic<FloatArray>(), "FloatArray", { it is FloatArray })

    @JsName("doubleArrayClass")
    val doubleArrayClass = PrimitiveKClassImpl(jsClassIntrinsic<DoubleArray>(), "DoubleArray", { it is DoubleArray })

    @JsName("functionClass")
    fun functionClass(arity: Int): KClassImpl<Any> {
        return functionClasses.get(arity) ?: run {
            val result = PrimitiveKClassImpl(jsClassIntrinsic<Function<*>>().unsafeCast<JsClass<Any>>(), "Function$arity",
                                             { jsTypeOf(it) === "function" && it.asDynamic().length === arity })
            functionClasses.asDynamic()[arity] = result
            result
        }
    }
}

private val functionClasses = arrayOfNulls<KClassImpl<Any>>(0)
