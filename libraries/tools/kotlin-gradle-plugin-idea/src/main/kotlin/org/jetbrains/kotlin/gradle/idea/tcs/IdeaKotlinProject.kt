/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.tcs

import org.jetbrains.kotlin.gradle.idea.IdeaKotlinFragment
import org.jetbrains.kotlin.tooling.core.HasMutableExtras
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.jetbrains.kotlin.tooling.core.MutableExtras
import java.io.Serializable

@IdeaKotlinModel
sealed interface IdeaKotlinProject : HasMutableExtras, Serializable {
    val coordinates: IdeaKotlinProjectCoordinates
    val targets: Set<IdeaKotlinTarget>
    val fragments: Set<IdeaKotlinFragment>

    /*
    Quick 'lookup'/'query' methods into the model.
     */
    fun getTargetOrNul(name: String): IdeaKotlinTarget?
    fun getTarget(name: String): IdeaKotlinTarget

    fun getCompilationOrNull(coordinates: IdeaKotlinCompilationCoordinates): IdeaKotlinCompilation?
    fun getCompilation(coordinates: IdeaKotlinCompilationCoordinates): IdeaKotlinCompilation

    fun getFragmentOrNull(name: String): IdeaKotlinFragment?
    fun getFragment(name: String): IdeaKotlinFragment
}

fun IdeaKotlinProject(
    coordinates: IdeaKotlinProjectCoordinates,
    targets: Set<IdeaKotlinTarget>,
    fragments: Set<IdeaKotlinFragment>,
    kotlinGradlePluginVersion: KotlinToolingVersion,
    extras: MutableExtras,
): IdeaKotlinProject = IdeaKotlinProjectImpl(
    coordinates = coordinates,
    targets = targets,
    fragments = fragments,
    extras = extras
)

private data class IdeaKotlinProjectImpl(
    override val coordinates: IdeaKotlinProjectCoordinates,
    override val targets: Set<IdeaKotlinTarget>,
    override val fragments: Set<IdeaKotlinFragment>,
    override val extras: MutableExtras,
) : IdeaKotlinProject {

    private val fragmentsByName = fragments.associateBy { it.fragmentName }

    private val targetsByName = targets.associateBy { it.targetName }

    private val compilationByCoordinates = targets.flatMap { it.compilations }
        .associateBy { it.coordinates }

    override fun getTargetOrNul(name: String): IdeaKotlinTarget? {
        return targetsByName[name]
    }

    override fun getTarget(name: String): IdeaKotlinTarget {
        return getTargetOrNul(name) ?: throw NoSuchElementException("target: $name not found in $coordinates")
    }

    override fun getCompilationOrNull(coordinates: IdeaKotlinCompilationCoordinates): IdeaKotlinCompilation? {
        return compilationByCoordinates[coordinates]
    }

    override fun getCompilation(coordinates: IdeaKotlinCompilationCoordinates): IdeaKotlinCompilation {
        return getCompilationOrNull(coordinates)
            ?: throw NoSuchElementException("compilation: $coordinates not found in ${this.coordinates}")
    }

    override fun getFragmentOrNull(name: String): IdeaKotlinFragment? {
        return fragmentsByName[name]
    }

    override fun getFragment(name: String): IdeaKotlinFragment {
        return fragmentsByName[name] ?: error("fragment: $name not found in ${this.coordinates}")
    }

    companion object {
        const val serialVersionUID = 0L
    }
}
