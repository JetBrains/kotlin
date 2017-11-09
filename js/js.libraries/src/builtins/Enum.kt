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

package kotlin

public class Enum<T : Enum<T>> : Comparable<Enum<T>> {
    @JsName("name$") private var _name: String = ""
    @JsName("ordinal$") private var _ordinal: Int = 0

    val name: String
        get() = _name

    val ordinal: Int
        get() = _ordinal

    override fun compareTo(other: Enum<T>) = ordinal.compareTo(other.ordinal)

    override fun equals(other: Any?) = this === other

    override fun hashCode(): Int = js("Kotlin.identityHashCode")(this)

    override fun toString() = name

    companion object
}