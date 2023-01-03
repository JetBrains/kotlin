/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.PublishArtifactSet
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.capabilities.Capability
import org.jetbrains.kotlin.gradle.plugin.ProjectLocalConfigurations

internal fun copyConfigurationForPublishing(
    project: Project,
    newName: String,
    configuration: Configuration,
    overrideDependencies: (DependencySet.() -> Unit)? = null,
    overrideArtifacts: (Configuration.(PublishArtifactSet) -> Unit)? = null,
    overrideAttributes: (Configuration.(AttributeContainer) -> Unit)? = {
        copyAttributes(it, attributes, it.keySet().minus(ProjectLocalConfigurations.ATTRIBUTE))
    },
    overrideCapabilities: (Configuration.(Collection<Capability>) -> Unit)? = null,
    configureAction: Configuration.() -> Unit = { }
) = copyConfiguration(
    project,
    newName,
    configuration,
    overrideDependencies,
    overrideArtifacts,
    overrideAttributes,
    overrideCapabilities,
    configureAction = {
        isCanBeConsumed = false
        isCanBeResolved = false
        configureAction()
    }
)

internal fun copyConfiguration(
    project: Project,
    newName: String,
    configuration: Configuration,
    overrideDependencies: (DependencySet.() -> Unit)? = null,
    overrideArtifacts: (Configuration.(PublishArtifactSet) -> Unit)? = null,
    overrideAttributes: (Configuration.(AttributeContainer) -> Unit)? = null,
    overrideCapabilities: (Configuration.(Collection<Capability>) -> Unit)? = null,
    configureAction: Configuration.() -> Unit = { }
): Configuration =
    project.configurations.create(newName).apply {
        isCanBeConsumed = configuration.isCanBeConsumed
        isCanBeResolved = configuration.isCanBeResolved

        // dependencies:
        if (overrideDependencies != null) {
            withDependencies { dependencySet ->
                overrideDependencies(dependencySet)
            }
        } else {
            dependencies.addAllLater(project.listProperty { configuration.allDependencies })
        }

        // artifacts:
        if (overrideArtifacts != null) {
            overrideArtifacts(this, artifacts)
        } else {
            artifacts.addAllLater(project.listProperty { configuration.allArtifacts })
        }

        // attributes
        if (overrideAttributes != null) {
            overrideAttributes(this, attributes)
        } else {
            copyAttributes(configuration.attributes, attributes)
        }

        // capabilities
        if (overrideCapabilities != null) {
            overrideCapabilities(this, configuration.outgoing.capabilities)
        } else {
            configuration.outgoing.capabilities.forEach(outgoing::capability)
        }

        configureAction(this)
    }

internal fun copyAttributes(from: AttributeContainer, to: AttributeContainer, keys: Iterable<Attribute<*>> = from.keySet()) {
    // capture type argument T
    fun <T : Any> copyOneAttribute(from: AttributeContainer, to: AttributeContainer, key: Attribute<T>) {
        val value = checkNotNull(from.getAttribute(key))
        to.attribute(key, value)
    }
    for (key in keys) {
        copyOneAttribute(from, to, key)
    }
}

internal inline fun <reified T> Project.listProperty(noinline itemsProvider: () -> Iterable<T>) =
    objects.listProperty(T::class.java).apply { set(provider(itemsProvider)) }


