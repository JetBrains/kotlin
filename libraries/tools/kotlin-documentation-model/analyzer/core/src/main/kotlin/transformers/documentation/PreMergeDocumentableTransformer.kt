package org.jetbrains.dokka.transformers.documentation

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.DokkaConfiguration.PackageOptions
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.WithExpectActual

interface PreMergeDocumentableTransformer {
    operator fun invoke(modules: List<DModule>): List<DModule>
}

/* Utils */

/**
 * It is fair to assume that a given [Documentable] is not merged when seen by the [PreMergeDocumentableTransformer].
 * Therefore, it can also be assumed, that there is just a single source set connected to the given [documentable]
 * @return the single source set associated with this [documentable].
 */
@Suppress("unused") // Receiver is used for scoping this function
fun PreMergeDocumentableTransformer.sourceSet(documentable: Documentable): DokkaSourceSet {
    return documentable.sourceSets.single()
}

/**
 * @return The [PackageOptions] associated with this documentable, or null
 */
fun PreMergeDocumentableTransformer.perPackageOptions(documentable: Documentable): PackageOptions? {
    val packageName = documentable.dri.packageName ?: return null
    return sourceSet(documentable).perPackageOptions
        .sortedByDescending { packageOptions -> packageOptions.prefix.length }
        .firstOrNull { packageOptions -> packageName.startsWith(packageOptions.prefix) }
}

fun <T> PreMergeDocumentableTransformer.source(documentable: T) where T : Documentable, T : WithExpectActual =
    checkNotNull(documentable.sources[sourceSet(documentable)])
