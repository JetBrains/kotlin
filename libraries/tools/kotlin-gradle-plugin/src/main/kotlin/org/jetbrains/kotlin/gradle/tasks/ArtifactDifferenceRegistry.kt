package org.jetbrains.kotlin.gradle.tasks

import org.jetbrains.kotlin.incremental.DirtyData
import java.io.File

internal interface ArtifactDifferenceRegistry {
    operator fun get(artifact: File): Iterable<ArtifactDifference>?
    fun add(artifact: File, difference: ArtifactDifference)
    fun remove(artifact: File)
}

internal class ArtifactDifference(val buildTS: Long, val dirtyData: DirtyData)