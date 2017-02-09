/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.w3c.dom

public external interface ItemArrayLike<out T> {
    val length: Int
    fun item(index: Int): T?
}

/**
 * Returns the view of this `ItemArrayLike<T>` collection as `List<T>`
 */
public fun <T> ItemArrayLike<T>.asList(): List<T> = object : AbstractList<T>() {
    override val size: Int get() = this@asList.length

    override fun get(index: Int): T = when {
        index in 0..lastIndex -> this@asList.item(index) as T
        else -> throw IndexOutOfBoundsException("index $index is not in range [0..$lastIndex]")
    }
}