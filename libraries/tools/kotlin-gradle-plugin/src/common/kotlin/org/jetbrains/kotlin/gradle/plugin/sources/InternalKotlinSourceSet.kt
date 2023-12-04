/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataCompilation
import org.jetbrains.kotlin.gradle.utils.MutableObservableSet
import org.jetbrains.kotlin.gradle.utils.ObservableSet

internal val KotlinSourceSet.internal: InternalKotlinSourceSet
    get() = (this as? InternalKotlinSourceSet) ?: throw IllegalArgumentException(
        "KotlinSourceSet $name (${this::class}) does not implement ${InternalKotlinSourceSet::class.simpleName}"
    )

internal interface InternalKotlinSourceSet : KotlinSourceSet {
    override val dependsOn: ObservableSet<KotlinSourceSet>
    val dependsOnClosure: ObservableSet<KotlinSourceSet>
    val withDependsOnClosure: ObservableSet<KotlinSourceSet>
    val compilations: MutableObservableSet<KotlinCompilation<*>>

    /** Configuration that resolves into sources variants of all Source Set dependencies */
    val dependencySourcesConfigurationName: String
}

internal suspend fun InternalKotlinSourceSet.awaitPlatformCompilations(): Set<KotlinCompilation<*>> {
    KotlinPluginLifecycle.Stage.AfterFinaliseRefinesEdges.await()
    return compilations.filter { it !is KotlinMetadataCompilation }.toSet()
}
