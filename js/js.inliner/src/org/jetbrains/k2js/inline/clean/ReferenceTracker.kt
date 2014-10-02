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

package org.jetbrains.k2js.inline.clean

import com.google.dart.compiler.backend.js.ast.*
import org.jetbrains.k2js.inline.util.IdentitySet

import java.util.IdentityHashMap
import java.util.ArrayList

private class ReferenceTracker<Reference, RemoveCandidate : JsNode> {
    private val reachable = IdentityHashMap<Reference, Boolean>()
    private val removableCandidates = IdentityHashMap<Reference, RemoveCandidate>()
    private val refereceFromTo = IdentityHashMap<Reference, MutableSet<Reference>>()
    private val visited = IdentitySet<Reference>()

    public val removable: List<RemoveCandidate>
        get() {
            return reachable
                        .filter { !it.value }
                        .map { removableCandidates.get(it.key)!! }
        }

    public fun addCandidateForRemoval(reference: Reference, candidate: RemoveCandidate) {
        assert(!isKnown(reference)) { "Candidate for removal cannot be reassigned: $candidate" }

        removableCandidates.put(reference, candidate)
        reachable.put(reference, false)
    }

    public fun addRemovableReference(referer: Reference, referenced: Reference) {
        if (!isKnown(referenced)) return

        getReferencedBy(referer).add(referenced)

        if (isReachable(referer)) {
            markReachable(referenced)
        }
    }

    public fun markReachable(reference: Reference) {
        if (!isKnown(reference)) return

        visited.add(reference)
        getReferencedBy(reference)
                .filterNot { it in visited }
                .filter { isKnown(it) && !isReachable(it) }
                .forEach { markReachable(it) }

        visited.remove(reference)
        reachable[reference] = true
    }

    private fun getReferencedBy(referer: Reference): MutableSet<Reference> {
        return refereceFromTo.getOrPut(referer, { IdentitySet<Reference>() })
    }

    private fun isKnown(ref: Reference): Boolean {
        return removableCandidates.containsKey(ref)
    }

    private fun isReachable(ref: Reference): Boolean {
        return reachable[ref] ?: false
    }
}
