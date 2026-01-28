/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.utils.MutableObservableSet
import org.jetbrains.kotlin.gradle.utils.MutableObservableSetImpl
import org.jetbrains.kotlin.gradle.utils.ObservableSet

internal fun KotlinCompilationSourceSetsContainer(
    defaultSourceSet: KotlinSourceSet
): KotlinCompilationSourceSetsContainer {
    return DefaultKotlinCompilationSourceSetsContainer(defaultSourceSet)
}

internal interface KotlinCompilationSourceSetsContainer {
    val defaultSourceSet: KotlinSourceSet
    val kotlinSourceSets: ObservableSet<KotlinSourceSet>
    val allKotlinSourceSets: ObservableSet<KotlinSourceSet>
    fun source(sourceSet: KotlinSourceSet)
}

private class DefaultKotlinCompilationSourceSetsContainer(
    override val defaultSourceSet: KotlinSourceSet
) : KotlinCompilationSourceSetsContainer {
    override val kotlinSourceSets: ObservableSet<KotlinSourceSet>
        field = MutableObservableSetImpl(defaultSourceSet)

    override val allKotlinSourceSets: ObservableSet<KotlinSourceSet>
        field = MutableObservableSetImpl<KotlinSourceSet>().also { set ->
            defaultSourceSet.internal.withDependsOnClosure.forAll(set::add)
        }

    /**
     * All SourceSets that have been processed by [source] already.
     * [directlyIncludedKotlinSourceSets] cannot be used in this case, because
     * the [defaultSourceSet] will always be already included.
     */
    private val sourcedKotlinSourceSets = hashSetOf<KotlinSourceSet>()

    override fun source(sourceSet: KotlinSourceSet) {
        if (!sourcedKotlinSourceSets.add(sourceSet)) return
        kotlinSourceSets.add(sourceSet)
        sourceSet.internal.withDependsOnClosure.forAll { inDependsOnClosure ->
            allKotlinSourceSets.add(inDependsOnClosure)
        }
    }
}
