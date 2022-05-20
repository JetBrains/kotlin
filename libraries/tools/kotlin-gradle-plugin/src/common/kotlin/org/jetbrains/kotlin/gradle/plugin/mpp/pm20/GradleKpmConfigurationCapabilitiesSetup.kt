/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationPublications
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmConfigurationCapabilitiesSetup.CapabilitiesContainer

interface GradleKpmConfigurationCapabilitiesSetup<in T : GradleKpmFragment> {
    interface CapabilitiesContainer {
        fun capability(notation: Any)
    }

    fun setCapabilities(container: CapabilitiesContainer, fragment: T)

    object None : GradleKpmConfigurationCapabilitiesSetup<GradleKpmFragment> {
        override fun setCapabilities(container: CapabilitiesContainer, fragment: GradleKpmFragment) = Unit
    }
}

fun <T : GradleKpmFragment> GradleKpmConfigurationCapabilitiesSetup<T>.setCapabilities(
    publications: ConfigurationPublications, fragment: T
) = setCapabilities(CapabilitiesContainer(publications), fragment)

fun CapabilitiesContainer(configuration: ConfigurationPublications): CapabilitiesContainer =
    CapabilitiesContainerImpl(configuration)

private class CapabilitiesContainerImpl(
    private val publications: ConfigurationPublications
) : CapabilitiesContainer {
    override fun capability(notation: Any) = publications.capability(notation)
}

class GradleKpmConfigurationCapabilitiesSetupContext<T : GradleKpmFragment> internal constructor(
    internal val container: CapabilitiesContainer, val fragment: T
) : CapabilitiesContainer by container {
    val project: Project get() = fragment.project
}

fun <T : GradleKpmFragment> GradleKpmConfigurationCapabilitiesSetup(
    setCapabilities: GradleKpmConfigurationCapabilitiesSetupContext<T>.() -> Unit
): GradleKpmConfigurationCapabilitiesSetup<T> = object : GradleKpmConfigurationCapabilitiesSetup<T> {
    override fun setCapabilities(container: CapabilitiesContainer, fragment: T) {
        val context = GradleKpmConfigurationCapabilitiesSetupContext(container, fragment)
        context.setCapabilities()
    }
}

operator fun <T : GradleKpmFragment> GradleKpmConfigurationCapabilitiesSetup<T>.plus(
    other: GradleKpmConfigurationCapabilitiesSetup<T>
): GradleKpmConfigurationCapabilitiesSetup<T> {
    if (this === GradleKpmConfigurationCapabilitiesSetup.None) return other
    if (other === GradleKpmConfigurationCapabilitiesSetup.None) return this

    if (this is CompositeKpmCapabilitiesSetup && other is CompositeKpmCapabilitiesSetup) {
        return CompositeKpmCapabilitiesSetup(this.children + other.children)
    }

    if (this is CompositeKpmCapabilitiesSetup) {
        return CompositeKpmCapabilitiesSetup(this.children + other)
    }

    if (other is CompositeKpmCapabilitiesSetup) {
        return CompositeKpmCapabilitiesSetup(listOf(this) + other.children)
    }

    return CompositeKpmCapabilitiesSetup(listOf(this, other))
}

internal class CompositeKpmCapabilitiesSetup<in T : GradleKpmFragment>(val children: List<GradleKpmConfigurationCapabilitiesSetup<T>>) :
    GradleKpmConfigurationCapabilitiesSetup<T> {
    override fun setCapabilities(container: CapabilitiesContainer, fragment: T) {
        children.forEach { child -> child.setCapabilities(container, fragment) }
    }
}
