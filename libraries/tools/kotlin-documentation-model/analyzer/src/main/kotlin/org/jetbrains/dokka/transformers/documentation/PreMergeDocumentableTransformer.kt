/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.transformers.documentation

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.DokkaConfiguration.PackageOptions
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.DocumentableSource
import org.jetbrains.dokka.model.WithSources

public interface PreMergeDocumentableTransformer {
    public operator fun invoke(modules: List<DModule>): List<DModule>
}

/**
 * It is fair to assume that a given [Documentable] is not merged when seen by the [PreMergeDocumentableTransformer].
 * Therefore, it can also be assumed, that there is just a single source set connected to the given [documentable]
 * @return the single source set associated with this [documentable].
 */
@Suppress("UnusedReceiverParameter") // Receiver is used for scoping this function
public fun PreMergeDocumentableTransformer.sourceSet(documentable: Documentable): DokkaSourceSet {
    return documentable.sourceSets.single()
}

/**
 * @return The [PackageOptions] associated with this documentable, or null
 */
public fun PreMergeDocumentableTransformer.perPackageOptions(documentable: Documentable): PackageOptions? {
    val packageName = documentable.dri.packageName ?: return null
    return sourceSet(documentable).perPackageOptions
        .sortedByDescending { packageOptions -> packageOptions.matchingRegex.length }
        .firstOrNull { packageOptions -> Regex(packageOptions.matchingRegex).matches(packageName) }
}

public fun <T> PreMergeDocumentableTransformer.source(documentable: T): DocumentableSource where T : Documentable, T : WithSources =
    checkNotNull(documentable.sources[sourceSet(documentable)])
