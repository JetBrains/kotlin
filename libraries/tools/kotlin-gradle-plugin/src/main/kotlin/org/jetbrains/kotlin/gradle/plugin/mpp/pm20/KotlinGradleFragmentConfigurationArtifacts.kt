/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.artifacts.ConfigurationPublications

/* Internal abbreviation */
internal typealias FragmentArtifacts<T> = KotlinGradleFragmentConfigurationArtifacts<T>

fun interface KotlinGradleFragmentConfigurationArtifacts<in T : KotlinGradleFragment> {
    fun ConfigurationPublications.addArtifacts(fragment: T)

    companion object {
        val None = FragmentArtifacts<KotlinGradleFragment> {}
    }
}

fun <T : KotlinGradleFragment> ConfigurationPublications.artifacts(
    artifacts: FragmentArtifacts<T>, fragment: T
) = with(artifacts) { addArtifacts(fragment) }

operator fun <T : KotlinGradleFragment> FragmentArtifacts<T>.plus(other: FragmentArtifacts<T>): FragmentArtifacts<T> {
    if (this is CompositeFragmentArtifacts && other is CompositeFragmentArtifacts) {
        return CompositeFragmentArtifacts(this.children + other.children)
    }

    if (this === FragmentArtifacts.None) return other
    if (other === FragmentArtifacts.None) return this

    if (this is CompositeFragmentArtifacts) {
        return CompositeFragmentArtifacts(this.children + other)
    }

    if (other is CompositeFragmentArtifacts) {
        return CompositeFragmentArtifacts(listOf(this) + other.children)
    }

    return CompositeFragmentArtifacts(listOf(this, other))
}

internal class CompositeFragmentArtifacts<in T : KotlinGradleFragment>(val children: List<FragmentArtifacts<T>>) :
    FragmentArtifacts<T> {
    override fun ConfigurationPublications.addArtifacts(fragment: T) {
        children.forEach { child -> artifacts(child, fragment) }
    }
}
