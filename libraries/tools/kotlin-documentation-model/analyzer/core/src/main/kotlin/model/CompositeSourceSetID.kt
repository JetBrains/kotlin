package org.jetbrains.dokka.model

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaSourceSetID

data class CompositeSourceSetID(
    private val children: Set<DokkaSourceSetID>
) {
    constructor(sourceSetIDs: Iterable<DokkaSourceSetID>) : this(sourceSetIDs.toSet())
    constructor(sourceSetId: DokkaSourceSetID) : this(setOf(sourceSetId))

    init {
        require(children.isNotEmpty()) { "Expected at least one source set id" }
    }

    val merged = DokkaSourceSetID(
        moduleName = children.map { it.moduleName }.reduce { acc, s -> "$acc+$s" },
        sourceSetName = children.map { it.sourceSetName }.reduce { acc, s -> "$acc+$s" }
    )

    val all: Set<DokkaSourceSetID> = setOf(merged, *children.toTypedArray())

    operator fun contains(sourceSetId: DokkaSourceSetID): Boolean {
        return sourceSetId in all
    }

    operator fun contains(sourceSet: DokkaConfiguration.DokkaSourceSet): Boolean {
        return sourceSet.sourceSetID in this
    }
}
