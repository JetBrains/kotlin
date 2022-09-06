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

    private val kotlinSourceSetsClosureImpl: MutableObservableSet<KotlinSourceSet> by lazy {
        MutableObservableSet<KotlinSourceSet>().also { set ->
            defaultSourceSet.internal.withDependsOnClosure.forAll(set::add)
        }
    }

    final override val kotlinSourceSetsClosure: ObservableSet<KotlinSourceSet>
        get() = kotlinSourceSetsClosureImpl

    final override fun source(sourceSet: KotlinSourceSet) {
        directlyIncludedKotlinSourceSetsImpl.add(sourceSet)
        sourceSet.internal.withDependsOnClosure.forAll { withDependsOn ->
            kotlinSourceSetsClosureImpl.add(withDependsOn)
        }

        whenSourceSetAdded(sourceSet)
    }

    /**
     * Called after [sourceSet] added the [sourceSet] to its respective ObservableSet's
     */
    protected open fun whenSourceSetAdded(sourceSet: KotlinSourceSet) = Unit
}
