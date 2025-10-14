/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.ArtifactView
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.artifacts.ResolvedConfiguration
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.result.*
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.tooling.core.withClosure

/**
 * Represents a Gradle Configuration that was resolved after configuration time.
 * But still can be accessed during Configuration time, triggering configuration resolution
 *
 * Serializable to configuration cache. So it can be stored in task state and be accessed during execution time.
 *
 * Has similar API as non-configuration cache friendly Gradle's [ResolvedConfiguration]
 */
internal class LazyResolvedConfigurationComponent private constructor(
    private val resolvedComponentsRootProvider: Lazy<ResolvedComponentResult>,
    val configurationName: String,
) {

    /**
     * Creates [LazyResolvedConfigurationWithArtifacts] from given [configuration].
     * The underlying ArtifactView can be configured with [configureArtifactView] or [configureArtifactViewAttributes]
     */
    constructor(
        configuration: Configuration,
    ) : this(
        // Calling resolutionResult doesn't actually trigger resolution. But accessing its root ResolvedComponentResult
        // via ResolutionResult::root does. ResolutionResult can't be serialised for Configuration Cache
        // but ResolvedComponentResult can. Wrapping it in `lazy` makes it resolve upon serialisation.
        resolvedComponentsRootProvider = configuration.incoming.resolutionResult.let { rr -> lazy { rr.root } },
        configurationName = configuration.name
    )

    val root by resolvedComponentsRootProvider

    val allDependencies: Set<DependencyResult> by TransientLazy {
        root.dependencies.withClosure<DependencyResult> {
            if (it is ResolvedDependencyResult) it.selected.dependencies
            else emptyList()
        }
    }

    internal val allResolvedDependencies: Set<ResolvedDependencyResult> by TransientLazy {
        allDependencies.filterIsInstance<ResolvedDependencyResult>().toSet()
    }

    override fun toString(): String = "LazyResolvedConfiguration(configuration='$configurationName')"
}

private fun Configuration.lazyArtifactCollection(configureArtifactView: ArtifactView.ViewConfiguration.() -> Unit): ArtifactCollection =
    incoming.artifactView { view ->
        view.isLenient = true
        view.configureArtifactView()
    }.artifacts

internal tailrec fun ResolvedVariantResult.lastExternalVariantOrSelf(): ResolvedVariantResult {
    return if (externalVariant.isPresent) externalVariant.get().lastExternalVariantOrSelf() else this
}
