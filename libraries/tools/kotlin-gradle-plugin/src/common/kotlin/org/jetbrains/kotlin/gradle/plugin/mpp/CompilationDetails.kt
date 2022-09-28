/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.*
import org.jetbrains.kotlin.gradle.plugin.sources.*
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.tooling.core.closure
import java.util.*

interface CompilationDetails<T : KotlinCommonOptions> {
    val target: KotlinTarget

    val compileDependencyFilesHolder: GradleKpmDependencyFilesHolder

    val kotlinDependenciesHolder: HasKotlinDependencies

    val compilationData: KotlinCompilationData<T>

    fun associateWith(other: CompilationDetails<*>)
    val associateCompilations: Set<CompilationDetails<*>>

    fun source(sourceSet: KotlinSourceSet)

    val kotlinSourceSets: ObservableSet<KotlinSourceSet>

    val allKotlinSourceSets: ObservableSet<KotlinSourceSet>

    val defaultSourceSet: KotlinSourceSet

    @Deprecated("Use defaultSourceSet.name instead", ReplaceWith("defaultSourceSet.name"), level = DeprecationLevel.WARNING)
    val defaultSourceSetName: String get() = defaultSourceSet.name

    @Suppress("UNCHECKED_CAST")
    val compilation: KotlinCompilation<T>
        get() = target.compilations.getByName(compilationData.compilationPurpose) as KotlinCompilation<T>
}

interface CompilationDetailsWithRuntime<T : KotlinCommonOptions> : CompilationDetails<T> {
    val runtimeDependencyFilesHolder: GradleKpmDependencyFilesHolder
}

internal val CompilationDetails<*>.associateCompilationsClosure: Iterable<CompilationDetails<*>>
    get() = closure { it.associateCompilations }


