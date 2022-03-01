/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.artifacts.ConfigurationPublications
import org.gradle.api.artifacts.ConfigurationVariant

/* Internal abbreviation */
internal typealias FragmentArtifacts<T> = KotlinGradleFragmentConfigurationArtifacts<T>

interface KotlinGradleFragmentConfigurationArtifacts<in T : KotlinGradleFragment> {
    fun addArtifacts(outgoing: ConfigurationPublications, fragment: T)

    object None : KotlinGradleFragmentConfigurationArtifacts<KotlinGradleFragment> {
        override fun addArtifacts(outgoing: ConfigurationPublications, fragment: KotlinGradleFragment) = Unit
    }
}

class KotlinGradleFragmentConfigurationArtifactsContext<T : KotlinGradleFragment> internal constructor(
    internal val outgoing: ConfigurationPublications,
    val fragment: T
) {
    val project: Project get() = fragment.project

    val variants: NamedDomainObjectContainer<ConfigurationVariant> get() = outgoing.variants

    fun artifact(notation: Any) {
        outgoing.artifact(notation)
    }

    fun artifact(notation: Any, configure: ConfigurablePublishArtifact.() -> Unit) {
        outgoing.artifact(notation, configure)
    }
}

@Suppress("FunctionName")
fun <T : KotlinGradleFragment> FragmentArtifacts(
    addArtifacts: KotlinGradleFragmentConfigurationArtifactsContext<T>.() -> Unit
): KotlinGradleFragmentConfigurationArtifacts<T> {
    return object : KotlinGradleFragmentConfigurationArtifacts<T> {
        override fun addArtifacts(outgoing: ConfigurationPublications, fragment: T) {
            val context = KotlinGradleFragmentConfigurationArtifactsContext(outgoing, fragment)
            context.addArtifacts()
        }
    }
}

operator fun <T : KotlinGradleFragment> FragmentArtifacts<T>.plus(other: FragmentArtifacts<T>): FragmentArtifacts<T> {
    if (this === KotlinGradleFragmentConfigurationArtifacts.None) return other
    if (other === KotlinGradleFragmentConfigurationArtifacts.None) return this

    if (this is CompositeFragmentArtifacts && other is CompositeFragmentArtifacts) {
        return CompositeFragmentArtifacts(this.children + other.children)
    }

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

    override fun addArtifacts(outgoing: ConfigurationPublications, fragment: T) {
        children.forEach { child -> child.addArtifacts(outgoing, fragment) }
    }
}
