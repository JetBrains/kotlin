/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

package com.intellij.stats.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.impl.LookupImpl

class LookupStateManager {
    private val elementToId = mutableMapOf<String, Int>()
    private val idToEntryInfo = mutableMapOf<Int, LookupEntryInfo>()

    fun update(lookup: LookupImpl, factorsUpdated: Boolean): LookupState {
        val ids = mutableListOf<Int>()
        val newIds = mutableSetOf<Int>()

        val items = lookup.items

        val currentPosition = items.indexOf(lookup.currentItem)

        val elementToId = mutableMapOf<LookupElement, Int>()
        for (item in items) {
            var id = getElementId(item)
            if (id == null) {
                id = registerElement(item)
                newIds.add(id)
            }
            elementToId[item] = id
            ids.add(id)
        }

        if (factorsUpdated) {
            val infos = items.toLookupInfos(lookup, elementToId)
            val newInfos = infos.filter { it.id in newIds }

            val itemsDiff = infos.mapNotNull { idToEntryInfo[it.id]?.calculateDiff(it) }
            infos.forEach { idToEntryInfo[it.id] = it }
            return LookupState(ids, newInfos, itemsDiff, currentPosition)
        }
        else {
            val newItems = items.filter { getElementId(it) in newIds }.toLookupInfos(lookup, elementToId)
            newItems.forEach { idToEntryInfo[it.id] = it }
            return LookupState(ids, newItems, emptyList(), currentPosition)
        }
    }


    fun getElementId(item: LookupElement): Int? {
        val itemString = item.idString()
        return elementToId[itemString]
    }

    private fun registerElement(item: LookupElement): Int {
        val itemString = item.idString()
        val newId = elementToId.size
        elementToId[itemString] = newId
        return newId
    }

    private fun List<LookupElement>.toLookupInfos(lookup: LookupImpl, elementToId: Map<LookupElement, Int>): List<LookupEntryInfo> {
        val relevanceObjects = lookup.getRelevanceObjects(this, false)
        return this.map { lookupElement ->
            val relevanceMap = relevanceObjects[lookupElement]?.let { objects ->
                RelevanceUtil.asRelevanceMap(objects).mapValues { entry -> entry.value.toString() }
            }

            val elementId = elementToId.getValue(lookupElement)
            LookupEntryInfo(elementId, lookupElement.lookupString.length, relevanceMap)
        }
    }
}