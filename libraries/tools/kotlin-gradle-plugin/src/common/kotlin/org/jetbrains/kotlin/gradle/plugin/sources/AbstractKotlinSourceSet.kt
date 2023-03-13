/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("LeakingThis")

package org.jetbrains.kotlin.gradle.plugin.sources

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.kotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.utils.MutableObservableSet
import org.jetbrains.kotlin.gradle.utils.MutableObservableSetImpl
import org.jetbrains.kotlin.gradle.utils.ObservableSet

abstract class AbstractKotlinSourceSet : InternalKotlinSourceSet {
    private val dependsOnImpl = MutableObservableSetImpl<KotlinSourceSet>()
    private val dependsOnClosureImpl = MutableObservableSetImpl<KotlinSourceSet>()
    private val withDependsOnClosureImpl = MutableObservableSetImpl<KotlinSourceSet>(this)

    final override val dependsOn: ObservableSet<KotlinSourceSet>
        get() = dependsOnImpl

    final override val dependsOnClosure: ObservableSet<KotlinSourceSet>
        get() = dependsOnClosureImpl

    final override val withDependsOnClosure: ObservableSet<KotlinSourceSet>
        get() = withDependsOnClosureImpl

    override val compilations: MutableObservableSet<KotlinCompilation<*>> = MutableObservableSetImpl()

    final override fun dependsOn(other: KotlinSourceSet) {
        if (other == this) return

        check(project.kotlinPluginLifecycle.stage <= KotlinPluginLifecycle.Stage.FinaliseRefinesEdges) {
            "Illegal 'dependsOn' call in stage '${project.kotlinPluginLifecycle.stage}'"
        }

        /*
        Circular dependsOn hierarchies are not allowed:
        Throw if this SourceSet is already present in the dependsOnClosure of 'other'
         */
        checkForCircularDependsOnEdges(other)

        /* Nothing to-do, if already added as dependency */
        if (!dependsOnImpl.add(other)) return

        /* Maintain dependsOn closure sets */
        other.internal.withDependsOnClosure.forAll { inDependsOnClosure ->
            this.dependsOnClosureImpl.add(inDependsOnClosure)
            this.withDependsOnClosureImpl.add(inDependsOnClosure)
        }

        afterDependsOnAdded(other)
    }

    protected open fun afterDependsOnAdded(other: KotlinSourceSet) = Unit
}

