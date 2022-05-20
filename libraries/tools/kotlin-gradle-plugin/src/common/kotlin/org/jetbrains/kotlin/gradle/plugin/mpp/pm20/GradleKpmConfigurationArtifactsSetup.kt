/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.artifacts.ConfigurationPublications
import org.gradle.api.artifacts.ConfigurationVariant


interface GradleKpmConfigurationArtifactsSetup<in T : GradleKpmFragment> {
    fun setupArtifacts(outgoing: ConfigurationPublications, fragment: T)

    object None : GradleKpmConfigurationArtifactsSetup<GradleKpmFragment> {
        override fun setupArtifacts(outgoing: ConfigurationPublications, fragment: GradleKpmFragment) = Unit
    }
}

class GradleKpmConfigurationArtifactsSetupContext<T : GradleKpmFragment> internal constructor(
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
fun <T : GradleKpmFragment> GradleKpmConfigurationArtifactsSetup(
    addArtifacts: GradleKpmConfigurationArtifactsSetupContext<T>.() -> Unit
): GradleKpmConfigurationArtifactsSetup<T> {
    return object : GradleKpmConfigurationArtifactsSetup<T> {
        override fun setupArtifacts(outgoing: ConfigurationPublications, fragment: T) {
            val context = GradleKpmConfigurationArtifactsSetupContext(outgoing, fragment)
            context.addArtifacts()
        }
    }
}

operator fun <T : GradleKpmFragment> GradleKpmConfigurationArtifactsSetup<T>.plus(
    other: GradleKpmConfigurationArtifactsSetup<T>
): GradleKpmConfigurationArtifactsSetup<T> {
    if (this === GradleKpmConfigurationArtifactsSetup.None) return other
    if (other === GradleKpmConfigurationArtifactsSetup.None) return this

    if (this is CompositeFragmentConfigurationArtifactsSetup && other is CompositeFragmentConfigurationArtifactsSetup) {
        return CompositeFragmentConfigurationArtifactsSetup(this.children + other.children)
    }

    if (this is CompositeFragmentConfigurationArtifactsSetup) {
        return CompositeFragmentConfigurationArtifactsSetup(this.children + other)
    }

    if (other is CompositeFragmentConfigurationArtifactsSetup) {
        return CompositeFragmentConfigurationArtifactsSetup(listOf(this) + other.children)
    }

    return CompositeFragmentConfigurationArtifactsSetup(listOf(this, other))
}

internal class CompositeFragmentConfigurationArtifactsSetup<in T : GradleKpmFragment>(
    val children: List<GradleKpmConfigurationArtifactsSetup<T>>
) : GradleKpmConfigurationArtifactsSetup<T> {

    override fun setupArtifacts(outgoing: ConfigurationPublications, fragment: T) {
        children.forEach { child -> child.setupArtifacts(outgoing, fragment) }
    }
}
