/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import java.util.AbstractList

private class HTMLCollectionListView(val collection: HTMLCollection) : AbstractList<HTMLElement>() {
    override fun size(): Int = collection.length

    override fun get(index: Int): HTMLElement =
            when {
                index in 0..size() - 1 -> collection.item(index) as HTMLElement
                else -> throw IndexOutOfBoundsException("index $index is not in range [0 .. ${size() - 1})")
            }
}

public fun HTMLCollection.asList(): List<HTMLElement> = HTMLCollectionListView(this)
public fun HTMLCollection?.toElementList(): List<Element> = this?.asList() ?: emptyList()

private class DOMTokenListView(val delegate: DOMTokenList) : AbstractList<String>() {
    override fun size(): Int = delegate.length

    override fun get(index: Int) =
            when {
                index in 0..size() - 1 -> delegate.item(index)!!
                else -> throw IndexOutOfBoundsException("index $index is not in range [0 .. ${size() - 1})")
            }
}

public fun DOMTokenList.asList(): List<String> = DOMTokenListView(this)