/*
 * Copyright 2000-2017 JetBrains s.r.o.
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


class LookupEntryInfo(val id: Int, val length: Int, val relevance: Map<String, String?>?) {
    // returns null if no difference found
    fun calculateDiff(newValue: LookupEntryInfo): LookupEntryDiff? {
        assert(id == newValue.id) { "Could not compare infos for differenece lookup elements" }
        if (this === newValue) return null
        if (relevance == null && newValue.relevance == null) {
            return null
        }

        return relevanceDiff(id, relevance ?: emptyMap(), newValue.relevance ?: emptyMap())
    }

    private fun relevanceDiff(id: Int, before: Map<String, String?>, after: Map<String, String?>): LookupEntryDiff? {
        val added = after.filter { it.key !in before }
        val removed = before.keys.filter { it !in after }
        val changed = after.filter { it.key in before && it.value != before[it.key] }

        if (changed.isEmpty() && added.isEmpty() && removed.isEmpty()) {
            return null
        }
        return LookupEntryDiff(id, added, changed, removed)
    }
}