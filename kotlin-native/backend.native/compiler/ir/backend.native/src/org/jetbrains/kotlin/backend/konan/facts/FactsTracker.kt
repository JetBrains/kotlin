/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.facts

import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.path
import java.util.concurrent.ConcurrentHashMap

internal typealias FactHash = ULong
internal typealias FileName = String

internal typealias Defs = Map<FactKey, FactHash>
internal typealias Uses = Map<FactKey, FactHash>

internal typealias DefsAssociation = Map<FileName, Defs>
internal typealias UsesAssociation = Map<FileName, Uses>

interface FactsTracker {
    val definedFacts: DefsAssociation
    val usedFacts: UsesAssociation

    fun defines(definee: IrFile, newFact: Fact)
    fun uses(user: IrFile, existingFact: Fact)
}

class FactsTrackerImpl(
        initialDefinedFacts: DefsAssociation = emptyMap(),
        initialUsedFacts: UsesAssociation = emptyMap(),
) : FactsTracker {

    override val definedFacts: DefsAssociation
            field: ConcurrentHashMap<FileName, ConcurrentHashMap<FactKey, FactHash>> = initialDefinedFacts.toConcurrentCopy()
    override val usedFacts: UsesAssociation
            field: ConcurrentHashMap<FileName, ConcurrentHashMap<FactKey, FactHash>> = initialUsedFacts.toConcurrentCopy()

    override fun defines(definee: IrFile, newFact: Fact) {
        val factKey = requireNotNull(newFact.factKey) {
            "Well, this should not happen for defines :)"
        }
        definedFacts.computeIfAbsent(definee.path) { ConcurrentHashMap() }[factKey] = newFact.contentHash()
    }

    override fun uses(user: IrFile, existingFact: Fact) {
        val factKey = requireNotNull(existingFact.factKey) {
            "Well, this should not happen for uses :)"
        }
        // Uses are recorded on lowered IR, so prefer the hash computed by [FactsCollector] before lowering.
        // Facts of other modules are not collected and their IR is not lowered, so they are hashed directly.
        val hash = definedFacts[existingFact.irFilePathString]?.get(factKey) ?: existingFact.contentHash()
        usedFacts.computeIfAbsent(user.path) { ConcurrentHashMap() }[factKey] = hash
    }
}

private fun Map<FileName, Map<FactKey, FactHash>>.toConcurrentCopy(): ConcurrentHashMap<FileName, ConcurrentHashMap<FactKey, FactHash>> =
        entries.associateTo(ConcurrentHashMap()) { [file, facts] -> file to ConcurrentHashMap(facts) }

