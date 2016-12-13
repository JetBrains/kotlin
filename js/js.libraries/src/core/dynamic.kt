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

public inline fun Any?.asDynamic(): dynamic = this

/**
 * Reinterprets this value as a value of the specified type [T] without any actual type checking.
 */
public inline fun <T> Any?.unsafeCast(): @kotlin.internal.NoInfer T = this.asDynamic()

// TODO add the support ES6 iterators
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
