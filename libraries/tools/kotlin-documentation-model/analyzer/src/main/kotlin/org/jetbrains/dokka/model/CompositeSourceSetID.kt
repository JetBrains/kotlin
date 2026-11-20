/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.model

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaSourceSetID

/**
 * A unique composite key of multiple [DokkaSourceSetID] that identifies [DisplaySourceSet].
 * Consists of multiple (non-zero) [DokkaSourceSetID] that the corresponding [DisplaySourceSet] was built from.
 *
 * Should not be constructed or copied outside of [DisplaySourceSet] instantiation.
 */
internal data class CompositeSourceSetID(
    private val children: Set<DokkaSourceSetID>
) {
    constructor(sourceSetIDs: Iterable<DokkaSourceSetID>) : this(sourceSetIDs.toSet())
    constructor(sourceSetId: DokkaSourceSetID) : this(setOf(sourceSetId))

    init {
        require(children.isNotEmpty()) { "Expected at least one source set id" }
    }

    val merged: DokkaSourceSetID = children.sortedBy { it.scopeId + it.sourceSetName }.let { sortedChildren ->
        DokkaSourceSetID(
            scopeId = sortedChildren.joinToString(separator = "+") { it.scopeId },
            sourceSetName = sortedChildren.joinToString(separator = "+") { it.sourceSetName }
        )
    }

    val all: Set<DokkaSourceSetID> = setOf(merged, *children.toTypedArray())

    operator fun contains(sourceSetId: DokkaSourceSetID): Boolean {
        return sourceSetId in all
    }

    operator fun contains(sourceSet: DokkaConfiguration.DokkaSourceSet): Boolean {
        return sourceSet.sourceSetID in this
    }

    operator fun plus(other: DokkaSourceSetID): CompositeSourceSetID {
        return copy(children = children + other)
    }
}
