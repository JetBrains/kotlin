/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// a package is omitted to get declarations directly under the module

import kotlin.reflect.KClass
import kotlin.reflect.js.internal.*

@JsName("getKClass")
internal fun <T : Any> getKClass(jClass: JsClass<T>): KClass<T> = getOrCreateKClass(jClass)
@JsName("getKClassFromExpression")
internal fun <T : Any> getKClassFromExpression(e: T): KClass<T> =
        when (jsTypeOf(e)) {
            "string" -> PrimitiveClasses.stringClass
            "number" -> if (js("e | 0") === e) PrimitiveClasses.intClass else PrimitiveClasses.doubleClass
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
                                getOrCreateKClass(jsClass)
                            }
                        }
                    }
                }
            }
        }.unsafeCast<KClass<T>>()

private fun <T : Any> getOrCreateKClass(jClass: JsClass<T>): KClass<T> {
    if (jClass === js("String")) return PrimitiveClasses.stringClass.unsafeCast<KClass<T>>()

    val metadata = jClass.asDynamic().`$metadata$`

    return if (metadata != null) {
        if (metadata.`$kClass$` == null) {
            val kClass = SimpleKClassImpl(jClass)
            metadata.`$kClass$` = kClass
            kClass
        }
        else {
            metadata.`$kClass$`
        }
    }
    else {
        SimpleKClassImpl(jClass)
    }
}

