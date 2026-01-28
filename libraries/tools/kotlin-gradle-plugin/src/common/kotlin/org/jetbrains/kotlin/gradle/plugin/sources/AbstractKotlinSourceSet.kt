/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("LeakingThis")

package org.jetbrains.kotlin.gradle.plugin.sources

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.hierarchy.redundantDependsOnEdgesTracker
import org.jetbrains.kotlin.gradle.plugin.kotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.utils.MutableObservableSet
import org.jetbrains.kotlin.gradle.utils.MutableObservableSetImpl
import org.jetbrains.kotlin.gradle.utils.ObservableSet

abstract class AbstractKotlinSourceSet : InternalKotlinSourceSet {

    final override val dependsOn: ObservableSet<KotlinSourceSet>
        field = MutableObservableSetImpl<KotlinSourceSet>()

    final override val dependsOnClosure: ObservableSet<KotlinSourceSet>
        field = MutableObservableSetImpl<KotlinSourceSet>()

    final override val withDependsOnClosure: ObservableSet<KotlinSourceSet>
        field = MutableObservableSetImpl<KotlinSourceSet>(this)

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

        project.multiplatformExtensionOrNull?.redundantDependsOnEdgesTracker?.remember(this, other)

        /* Nothing to-do, if already added as dependency */
        if (!dependsOn.add(other)) return

        /* Maintain dependsOn closure sets */
        other.internal.withDependsOnClosure.forAll { inDependsOnClosure ->
            this.dependsOnClosure.add(inDependsOnClosure)
            this.withDependsOnClosure.add(inDependsOnClosure)
        }

        afterDependsOnAdded(other)
    }

    protected open fun afterDependsOnAdded(other: KotlinSourceSet) = Unit
}

