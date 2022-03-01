/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.kpm

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationPublications
import org.jetbrains.kotlin.gradle.kpm.KotlinGradleFragmentConfigurationCapabilities.CapabilitiesContainer

/* Internal abbreviation */
internal typealias FragmentCapabilities<T> = KotlinGradleFragmentConfigurationCapabilities<T>

interface KotlinGradleFragmentConfigurationCapabilities<in T : KotlinGradleFragment> {
    interface CapabilitiesContainer {
        fun capability(notation: Any)
    }

    fun setCapabilities(container: CapabilitiesContainer, fragment: T)

    object None : KotlinGradleFragmentConfigurationCapabilities<KotlinGradleFragment> {
        override fun setCapabilities(container: CapabilitiesContainer, fragment: KotlinGradleFragment) = Unit
    }
}

fun <T : KotlinGradleFragment> KotlinGradleFragmentConfigurationCapabilities<T>.setCapabilities(
    publications: ConfigurationPublications, fragment: T
) = setCapabilities(CapabilitiesContainer(publications), fragment)

fun CapabilitiesContainer(configuration: ConfigurationPublications): CapabilitiesContainer =
    CapabilitiesContainerImpl(configuration)

fun CapabilitiesContainer(configuration: Configuration): CapabilitiesContainer =
    CapabilitiesContainerImpl(configuration.outgoing)

private class CapabilitiesContainerImpl(
    private val publications: ConfigurationPublications
) : CapabilitiesContainer {
    override fun capability(notation: Any) = publications.capability(notation)
}

class KotlinGradleFragmentConfigurationCapabilitiesContext<T : KotlinGradleFragment> internal constructor(
    internal val container: CapabilitiesContainer, val fragment: T
) : CapabilitiesContainer by container {
    val project: Project get() = fragment.project
}

fun <T : KotlinGradleFragment> FragmentCapabilities(
    setCapabilities: KotlinGradleFragmentConfigurationCapabilitiesContext<T>.() -> Unit
): KotlinGradleFragmentConfigurationCapabilities<T> = object : KotlinGradleFragmentConfigurationCapabilities<T> {
    override fun setCapabilities(container: CapabilitiesContainer, fragment: T) {
        val context = KotlinGradleFragmentConfigurationCapabilitiesContext(container, fragment)
        context.setCapabilities()
    }
}

operator fun <T : KotlinGradleFragment> FragmentCapabilities<T>.plus(other: FragmentCapabilities<T>): FragmentCapabilities<T> {
    if (this === KotlinGradleFragmentConfigurationCapabilities.None) return other
    if (other === KotlinGradleFragmentConfigurationCapabilities.None) return this

    if (this is CompositeFragmentCapabilities && other is CompositeFragmentCapabilities) {
        return CompositeFragmentCapabilities(this.children + other.children)
    }

    if (this is CompositeFragmentCapabilities) {
        return CompositeFragmentCapabilities(this.children + other)
    }

    if (other is CompositeFragmentCapabilities) {
        return CompositeFragmentCapabilities(listOf(this) + other.children)
    }

    return CompositeFragmentCapabilities(listOf(this, other))
}

internal class CompositeFragmentCapabilities<in T : KotlinGradleFragment>(val children: List<FragmentCapabilities<T>>) :
    FragmentCapabilities<T> {
    override fun setCapabilities(container: CapabilitiesContainer, fragment: T) {
        children.forEach { child -> child.setCapabilities(container, fragment) }
    }
}
