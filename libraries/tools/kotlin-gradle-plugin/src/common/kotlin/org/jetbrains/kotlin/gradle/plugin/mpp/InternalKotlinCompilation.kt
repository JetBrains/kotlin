/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationConfigurationsContainer
import org.jetbrains.kotlin.gradle.utils.ObservableSet
import org.jetbrains.kotlin.tooling.core.HasMutableExtras

@Suppress("DEPRECATION")
@InternalKotlinGradlePluginApi
internal interface InternalKotlinCompilation<T : KotlinAnyOptionsDeprecated> : KotlinCompilation<T>, HasMutableExtras {
    override val kotlinSourceSets: ObservableSet<KotlinSourceSet>
    override val allKotlinSourceSets: ObservableSet<KotlinSourceSet>

    override val associatedCompilations: ObservableSet<KotlinCompilation<*>>
    override val allAssociatedCompilations: ObservableSet<KotlinCompilation<*>>

    val configurations: KotlinCompilationConfigurationsContainer
    val friendPaths: Iterable<FileCollection>
    val processResourcesTaskName: String?

    /**
     * @see [org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationArchiveTasks]
     */
    val archiveTaskName: String?
}

@Suppress("DEPRECATION")
internal val <T : KotlinAnyOptionsDeprecated> KotlinCompilation<T>.internal: InternalKotlinCompilation<T>
    get() = (this as? InternalKotlinCompilation<T>) ?: throw IllegalArgumentException(
        "KotlinCompilation($name) ${this::class} does not implement ${InternalKotlinCompilation::class}"
    )

internal suspend fun InternalKotlinCompilation<*>.awaitAllKotlinSourceSets(): Set<KotlinSourceSet> {
    KotlinPluginLifecycle.Stage.AfterFinaliseCompilations.await()
    return allKotlinSourceSets
}

@Deprecated(
    "KT-58234: Adding source sets to Compilation is not recommended. Please consider using dependsOn. Scheduled for removal in Kotlin 2.3.",
    level = DeprecationLevel.ERROR
)
internal fun KotlinCompilation<out KotlinAnyOptionsDeprecated>.addSourceSet(kotlinSourceSet: KotlinSourceSet) {
    internal.decoratedInstance.compilation.sourceSets.source(kotlinSourceSet)
}

/**
 * Declaring dependencies and thus having configurations on [KotlinCompilation] level is deprecated.
 * However, to keep backward compatibility, KGP still has to configure them.
 * These `legacy*ConfigurationName` accessors are explicit opt-ins for cases when it is necessary to configure those configurations.
 * In other cases, configurations from `compilation.defaultSourceSet` should be used.
 *
 * After KT-81136 is implemented, and compilation-level configurations are removed, replace these methods
 * to use [KotlinCompilation.defaultSourceSet] configurations directly.
 */
internal val KotlinCompilation<*>.legacyApiConfigurationName: String
    get() {
        @Suppress("DEPRECATION")
        return apiConfigurationName
    }

internal val KotlinCompilation<*>.legacyImplementationConfigurationName: String
    get() {
        @Suppress("DEPRECATION")
        return implementationConfigurationName
    }

internal val KotlinCompilation<*>.legacyCompileOnlyConfigurationName: String
    get() {
        @Suppress("DEPRECATION")
        return compileOnlyConfigurationName
    }

internal val KotlinCompilation<*>.legacyRuntimeOnlyConfigurationName: String
    get() {
        @Suppress("DEPRECATION")
        return runtimeOnlyConfigurationName
    }