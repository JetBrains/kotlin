/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration

interface KotlinDependencyConfigurations {
    val apiConfiguration: NamedDomainObjectProvider<Configuration>
    val implementationConfiguration: NamedDomainObjectProvider<Configuration>
    val compileOnlyConfiguration: NamedDomainObjectProvider<Configuration>
    val runtimeOnlyConfiguration: NamedDomainObjectProvider<Configuration>

    /** This configuration includes the dependencies from the refines-parents */
    val transitiveApiConfiguration: NamedDomainObjectProvider<Configuration>

    /** This configuration includes the dependencies from the refines-parents */
    val transitiveImplementationConfiguration: NamedDomainObjectProvider<Configuration>

    private class Impl(
        override val apiConfiguration: NamedDomainObjectProvider<Configuration>,
        override val implementationConfiguration: NamedDomainObjectProvider<Configuration>,
        override val compileOnlyConfiguration: NamedDomainObjectProvider<Configuration>,
        override val runtimeOnlyConfiguration: NamedDomainObjectProvider<Configuration>,
        override val transitiveApiConfiguration: NamedDomainObjectProvider<Configuration>,
        override val transitiveImplementationConfiguration: NamedDomainObjectProvider<Configuration>
    ) : KotlinDependencyConfigurations

    companion object {
        fun create(
            apiConfiguration: NamedDomainObjectProvider<Configuration>,
            implementationConfiguration: NamedDomainObjectProvider<Configuration>,
            compileOnlyConfiguration: NamedDomainObjectProvider<Configuration>,
            runtimeOnlyConfiguration: NamedDomainObjectProvider<Configuration>,
            transitiveApiConfiguration: NamedDomainObjectProvider<Configuration>,
            transitiveImplementationConfiguration: NamedDomainObjectProvider<Configuration>
        ): KotlinDependencyConfigurations = Impl(
            apiConfiguration = apiConfiguration,
            implementationConfiguration = implementationConfiguration,
            compileOnlyConfiguration = compileOnlyConfiguration,
            runtimeOnlyConfiguration = runtimeOnlyConfiguration,
            transitiveApiConfiguration = transitiveApiConfiguration,
            transitiveImplementationConfiguration = transitiveImplementationConfiguration
        )
    }
}
