/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.artifacts.Configuration

interface GradleKpmFragmentDependencyConfigurations : GradleKpmDependencyConfigurations {
    /** This configuration includes the dependencies from the refines-parents */
    val transitiveApiConfiguration: Configuration

    /** This configuration includes the dependencies from the refines-parents */
    val transitiveImplementationConfiguration: Configuration

    /** This configuration includes the dependencies from the refines-parents */
    val transitiveRuntimeOnlyConfiguration: Configuration

    private class Impl(
        override val apiConfiguration: Configuration,
        override val implementationConfiguration: Configuration,
        override val compileOnlyConfiguration: Configuration,
        override val runtimeOnlyConfiguration: Configuration,
        override val transitiveApiConfiguration: Configuration,
        override val transitiveImplementationConfiguration: Configuration,
        override val transitiveRuntimeOnlyConfiguration: Configuration
    ) : GradleKpmFragmentDependencyConfigurations

    companion object {
        fun create(
            apiConfiguration: Configuration,
            implementationConfiguration: Configuration,
            compileOnlyConfiguration: Configuration,
            runtimeOnlyConfiguration: Configuration,
            transitiveApiConfiguration: Configuration,
            transitiveImplementationConfiguration: Configuration,
            transitiveRuntimeOnlyConfiguration: Configuration
        ): GradleKpmFragmentDependencyConfigurations = Impl(
            apiConfiguration = apiConfiguration,
            implementationConfiguration = implementationConfiguration,
            compileOnlyConfiguration = compileOnlyConfiguration,
            runtimeOnlyConfiguration = runtimeOnlyConfiguration,
            transitiveApiConfiguration = transitiveApiConfiguration,
            transitiveImplementationConfiguration = transitiveImplementationConfiguration,
            transitiveRuntimeOnlyConfiguration = transitiveRuntimeOnlyConfiguration
        )
    }
}
