/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.copyAttributes
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.markResolvable
import org.jetbrains.kotlin.gradle.plugin.sources.*
import org.jetbrains.kotlin.gradle.targets.metadata.ALL_COMPILE_METADATA_CONFIGURATION_NAME
import org.jetbrains.kotlin.tooling.core.extrasLazyProperty

/**
 * Represents a 'resolvable' configuration containing all dependencies in compile scope.
 * These dependencies are set up to resolve Kotlin Metadata (without transformation) and will resolve
 * consistently across the whole project.
 *
 * Creating this configuration will resolve dependency scope configurations on the given SourceSet.
 */
internal val InternalKotlinSourceSet.resolvableMetadataConfiguration: Configuration by extrasLazyProperty(
    "resolvableMetadataConfiguration"
) {
    /* Create new 'platform like compileDependencies configuration */
    val configuration = project.configurations.maybeCreate(disambiguateName("resolvable$METADATA_CONFIGURATION_NAME_SUFFIX"))
    configuration.markResolvable()

    ((getVisibleSourceSetsFromAssociateCompilations(this) + this).withDependsOnClosure).forEach { visibleSourceSet ->
        configuration.dependencies.addAll(
            project.configurations.getByName(visibleSourceSet.apiConfigurationName).incoming.dependencies
        )

        configuration.dependencies.addAll(
            project.configurations.getByName(visibleSourceSet.implementationConfigurationName).incoming.dependencies
        )

        configuration.dependencies.addAll(
            project.configurations.getByName(visibleSourceSet.compileOnlyConfigurationName).incoming.dependencies
        )
    }

    val allCompileMetadataConfiguration = project.configurations.getByName(ALL_COMPILE_METADATA_CONFIGURATION_NAME)

    /* Ensure consistent dependency resolution result within the whole module */
    configuration.shouldResolveConsistentlyWith(allCompileMetadataConfiguration)
    copyAttributes(allCompileMetadataConfiguration.attributes, configuration.attributes)

    configuration
}
