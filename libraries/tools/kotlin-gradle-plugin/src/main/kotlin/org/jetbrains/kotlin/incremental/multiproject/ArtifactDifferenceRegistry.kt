package org.jetbrains.kotlin.incremental.multiproject

import org.jetbrains.kotlin.incremental.DirtyData
import org.jetbrains.kotlin.incremental.IncReporter
import java.io.File

internal interface ArtifactDifferenceRegistry {
    operator fun get(artifact: File): Iterable<ArtifactDifference>?
    fun add(artifact: File, difference: ArtifactDifference)
    fun remove(artifact: File)
    fun flush(memoryCachesOnly: Boolean)
}

internal class ArtifactDifference(val buildTS: Long, val dirtyData: DirtyData)

internal interface ArtifactDifferenceRegistryProvider {
    fun <T> withRegistry(
            report: (String) -> Unit,
            fn: (ArtifactDifferenceRegistry) -> T
    ): T?

    fun <T> withRegistry(
            reporter: IncReporter,
            fn: (ArtifactDifferenceRegistry) -> T
    ): T? {
        return withRegistry({reporter.report {it}}, fn)
    }

    fun clean()
}
