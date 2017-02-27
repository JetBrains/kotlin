/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package kotlin.js

/**
 * Reinterprets this value as a value of the [dynamic type](/docs/reference/dynamic-type.html).
 */
@kotlin.internal.InlineOnly
public inline fun Any?.asDynamic(): dynamic = this

/**
 * Reinterprets this value as a value of the specified type [T] without any actual type checking.
 */
@kotlin.internal.InlineOnly
public inline fun <T> Any?.unsafeCast(): @kotlin.internal.NoInfer T = this.asDynamic()

/**
 * Reinterprets this `dynamic` value as a value of the specified type [T] without any actual type checking.
 */
@kotlin.internal.DynamicExtension
@JsName("unsafeCastDynamic")
@kotlin.internal.InlineOnly
public inline fun <T> dynamic.unsafeCast(): @kotlin.internal.NoInfer T = this

/**
 * Allows to iterate this `dynamic` object in the following cases:
 * - when it has an `iterator` function,
 * - when it is an array
 * - when it is an instance of [kotlin.collections.Iterable]
 */
@kotlin.internal.DynamicExtension
public operator fun dynamic.iterator(): Iterator<dynamic> {
    val r: Any? = this

    return when {
        this["iterator"] != null ->
            this["iterator"]()
        js("Array").isArray(r) ->
            r.unsafeCast<Array<*>>().iterator()

        else ->
            (r as Iterable<*>).iterator()
    }
}
