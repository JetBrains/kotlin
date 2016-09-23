package org.jetbrains.kotlin.gradle.tasks

import org.jetbrains.kotlin.incremental.DirtyData
import java.io.File

interface ArtifactDifferenceRegistry {
    operator fun get(artifact: File): Iterable<ArtifactDifference>?
    fun add(artifact: File, difference: ArtifactDifference)
    fun remove(artifact: File)
}

class ArtifactDifference(val buildTS: Long, val dirtyData: DirtyData)