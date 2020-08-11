package org.jetbrains.dokka.transformers.documentation

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.DocumentableSource
import org.jetbrains.dokka.model.WithExpectActual

interface PreMergeDocumentableTransformer {
    operator fun invoke(modules: List<DModule>): List<DModule>

    /* Convenience functions */
    /**
     * A [PreMergeDocumentableTransformer] can safely assume that documentables are not merged and therefore
     * only belong to a single source set
     */
    val Documentable.sourceSet: DokkaSourceSet get() = sourceSets.single()

    val Documentable.perPackageOptions: DokkaConfiguration.PackageOptions?
        get() {
            val packageName = dri.packageName ?: return null
            return sourceSet.perPackageOptions
                .sortedByDescending { packageOptions -> packageOptions.prefix.length }
                .firstOrNull { packageOptions -> packageName.startsWith(packageOptions.prefix) }
        }

    val <T> T.source: DocumentableSource where T : Documentable, T : WithExpectActual
        get() = checkNotNull(sources[sourceSet])
}
