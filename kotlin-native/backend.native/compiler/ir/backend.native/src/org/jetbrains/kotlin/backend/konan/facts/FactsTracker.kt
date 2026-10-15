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

class FactsTrackerImpl : FactsTracker {

    override val definedFacts: DefsAssociation
            field: ConcurrentHashMap<String, ConcurrentHashMap<FactKey, FactHash>> = ConcurrentHashMap()
    override val usedFacts: UsesAssociation
            field: ConcurrentHashMap<String, ConcurrentHashMap<FactKey, FactHash>> = ConcurrentHashMap()

    override fun defines(definee: IrFile, newFact: Fact) {
        val factKey = requireNotNull(newFact.factKey) {
            "Well, this should not happen for defines :)"
        }
        definedFacts.getOrPut(definee.path) { ConcurrentHashMap() }[factKey] = newFact.contentHash()
    }

    override fun uses(user: IrFile, existingFact: Fact) {
        val factKey = requireNotNull(existingFact.factKey) {
            "Well, this should not happen for uses :)"
        }
        usedFacts.getOrPut(user.path) { ConcurrentHashMap() }[factKey] = existingFact.contentHash()
    }
}

