/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationConfigurationsContainer
import org.jetbrains.kotlin.gradle.utils.ObservableSet
import org.jetbrains.kotlin.tooling.core.HasMutableExtras

internal interface InternalKotlinCompilation<out T : KotlinCommonOptions> : KotlinCompilation<T>, HasMutableExtras {
    override val kotlinSourceSets: ObservableSet<KotlinSourceSet>
    override val allKotlinSourceSets: ObservableSet<KotlinSourceSet>

    val configurations: KotlinCompilationConfigurationsContainer
    val friendPaths: Iterable<FileCollection>
    val processResourcesTaskName: String?
}

internal val <T : KotlinCommonOptions> KotlinCompilation<T>.internal: InternalKotlinCompilation<T>
    get() = (this as? InternalKotlinCompilation<T>) ?: throw IllegalArgumentException(
        "KotlinCompilation($name) ${this::class} does not implement ${InternalKotlinCompilation::class}"
    )