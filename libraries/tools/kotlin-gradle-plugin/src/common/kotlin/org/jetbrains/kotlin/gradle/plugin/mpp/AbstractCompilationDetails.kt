/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.utils.MutableObservableSet
import org.jetbrains.kotlin.gradle.utils.ObservableSet

abstract class AbstractCompilationDetails<T : KotlinCommonOptions> : CompilationDetails<T> {
    private val directlyIncludedKotlinSourceSetsImpl: MutableObservableSet<KotlinSourceSet> by lazy {
        MutableObservableSet(defaultSourceSet)
    }

    final override val directlyIncludedKotlinSourceSets: ObservableSet<KotlinSourceSet>
        get() = directlyIncludedKotlinSourceSetsImpl

    private val allKotlinSourceSetsImpl: MutableObservableSet<KotlinSourceSet> by lazy {
        MutableObservableSet<KotlinSourceSet>().also { set ->
            defaultSourceSet.internal.withDependsOnClosure.forAll(set::add)
        }
    }

    final override val allKotlinSourceSets: ObservableSet<KotlinSourceSet>
        get() = allKotlinSourceSetsImpl

    final override fun source(sourceSet: KotlinSourceSet) {
        directlyIncludedKotlinSourceSetsImpl.add(sourceSet)
        sourceSet.internal.withDependsOnClosure.forAll { withDependsOn ->
            allKotlinSourceSetsImpl.add(withDependsOn)
            withDependsOn.internal.addCompilation(compilation)
        }

        whenSourceSetAdded(sourceSet)
    }

    /**
     * Called after [sourceSet] added the [sourceSet] to its respective ObservableSet's
     */
    protected open fun whenSourceSetAdded(sourceSet: KotlinSourceSet) = Unit
}
