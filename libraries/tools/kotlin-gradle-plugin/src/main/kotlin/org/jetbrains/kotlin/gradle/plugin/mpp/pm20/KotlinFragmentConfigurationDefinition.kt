/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationPublications
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.capabilities.Capability
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.KotlinNameDisambiguation

/* Internal abbreviation */
internal typealias ConfigurationDefinition<T> = KotlinGradleFragmentConfigurationDefinition<T>

data class KotlinGradleFragmentConfigurationDefinition<in T : KotlinGradleFragment>(
    val provider: KotlinGradleFragmentConfigurationProvider,
    val attributes: KotlinGradleFragmentConfigurationAttributes<T> = KotlinGradleFragmentConfigurationAttributes.None,
    val capabilities: KotlinGradleFragmentConfigurationCapabilities<T> = KotlinGradleFragmentConfigurationCapabilities.None,
    val artifacts: KotlinGradleFragmentConfigurationArtifacts<T> = KotlinGradleFragmentConfigurationArtifacts.None,
    val relations: KotlinGradleFragmentConfigurationRelation = KotlinGradleFragmentConfigurationRelation.None,
)

/* Internal abbreviation */
internal typealias ConfigurationContext = KotlinGradleFragmentConfigurationContext

interface KotlinGradleFragmentConfigurationContext : KotlinNameDisambiguation {
    val project: Project get() = module.project
    val module: KotlinGradleModule
    val dependencies: KotlinFragmentDependencyConfigurations
}

internal class ConfigurationContextImpl(
    override val module: KotlinGradleModule,
    override val dependencies: KotlinFragmentDependencyConfigurations,
    names: KotlinNameDisambiguation
) : KotlinGradleFragmentConfigurationContext, KotlinNameDisambiguation by names

/* Internal abbreviation */
internal typealias FragmentConfigurationProvider = KotlinGradleFragmentConfigurationProvider

fun interface KotlinGradleFragmentConfigurationProvider {
    fun KotlinGradleFragmentConfigurationContext.getConfiguration(): Configuration
}

/* Internal abbreviation */
internal typealias FragmentRelation = KotlinGradleFragmentConfigurationRelation

fun interface KotlinGradleFragmentConfigurationRelation {
    fun KotlinGradleFragmentConfigurationContext.setupExtendsFromRelations(configuration: Configuration)

    object None : KotlinGradleFragmentConfigurationRelation {
        override fun KotlinGradleFragmentConfigurationContext.setupExtendsFromRelations(configuration: Configuration) = Unit
    }
}

/* Internal abbreviation */
internal typealias FragmentAttributes<T> = KotlinGradleFragmentConfigurationAttributes<T>

fun interface KotlinGradleFragmentConfigurationAttributes<in T : KotlinGradleFragment> {
    fun AttributeContainer.setAttributes(fragment: T)

    object None : KotlinGradleFragmentConfigurationAttributes<KotlinGradleFragment> {
        override fun AttributeContainer.setAttributes(fragment: KotlinGradleFragment) = Unit
    }
}
/* Internal abbreviation */
internal typealias FragmentCapabilities<T> = KotlinGradleFragmentConfigurationCapabilities<T>

fun interface KotlinGradleFragmentConfigurationCapabilities<in T : KotlinGradleFragment> {
    interface Context {
        fun capability(notation: Any)
        val capabilities: List<Capability>
    }

    class ContextImpl internal constructor(
        private val configuration: Configuration
    ) : Context {
        override fun capability(notation: Any) = configuration.outgoing.capability(notation)
        override val capabilities: List<Capability> get() = configuration.outgoing.capabilities.orEmpty().toList()
    }

    fun Context.setCapabilities(fragment: T)

    object None : KotlinGradleFragmentConfigurationCapabilities<KotlinGradleFragment> {
        override fun Context.setCapabilities(fragment: KotlinGradleFragment) = Unit
    }
}

/* Internal abbreviation */
internal typealias FragmentArtifacts<T> = KotlinGradleFragmentConfigurationArtifacts<T>

fun interface KotlinGradleFragmentConfigurationArtifacts<in T : KotlinGradleFragment> {
    fun ConfigurationPublications.addArtifacts(fragment: T)

    object None : KotlinGradleFragmentConfigurationArtifacts<KotlinGradleFragment> {
        override fun ConfigurationPublications.addArtifacts(fragment: KotlinGradleFragment) = Unit
    }
}

/*
Definition Extensions
 */

operator fun <T : KotlinGradleFragment> KotlinGradleFragmentConfigurationDefinition<T>.plus(other: FragmentAttributes<T>):
        KotlinGradleFragmentConfigurationDefinition<T> = copy(attributes = attributes + other)

operator fun <T : KotlinGradleFragment> KotlinGradleFragmentConfigurationDefinition<T>.plus(other: FragmentArtifacts<T>):
        KotlinGradleFragmentConfigurationDefinition<T> = copy(artifacts = artifacts + other)

/*
Provider Extensions
 */

fun <T : KotlinGradleFragment> KotlinGradleFragmentConfigurationDefinition<T>.withConfigurationProvider(
    provider: KotlinGradleFragmentConfigurationContext.() -> Configuration
) = copy(provider = provider)

fun KotlinGradleFragmentConfigurationProvider.getConfiguration(context: KotlinGradleFragmentConfigurationContext): Configuration {
    return with(context) { getConfiguration() }
}

/*
FragmentArtifacts Extensions
 */

fun <T : KotlinGradleFragment> ConfigurationPublications.addArtifacts(artifacts: FragmentArtifacts<T>, fragment: T) = with(artifacts) {
    addArtifacts(fragment)
}

operator fun <T : KotlinGradleFragment> FragmentArtifacts<T>.plus(other: FragmentArtifacts<T>): FragmentArtifacts<T> {
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
    override fun ConfigurationPublications.addArtifacts(fragment: T) {
        children.forEach { child -> addArtifacts(child, fragment) }
    }
}

/*
FragmentAttributes Extensions
 */

fun <T : KotlinGradleFragment> AttributeContainer.attribute(attributes: FragmentAttributes<T>, fragment: T) = with(attributes) {
    setAttributes(fragment)
}

operator fun <T : KotlinGradleFragment> FragmentAttributes<T>.plus(other: FragmentAttributes<T>): FragmentAttributes<T> {
    if (this is CompositeFragmentAttributes && other is CompositeFragmentAttributes) {
        return CompositeFragmentAttributes(this.children + other.children)
    }

    if (this is CompositeFragmentAttributes) {
        return CompositeFragmentAttributes(this.children + other)
    }

    if (other is CompositeFragmentAttributes) {
        return CompositeFragmentAttributes(listOf(this) + other.children)
    }

    return CompositeFragmentAttributes(listOf(this, other))
}

internal class CompositeFragmentAttributes<in T : KotlinGradleFragment>(val children: List<FragmentAttributes<T>>) :
    FragmentAttributes<T> {
    override fun AttributeContainer.setAttributes(fragment: T) {
        children.forEach { attribute -> attribute(attribute, fragment) }
    }
}

/*
FragmentCapabilities Extensions
 */

fun <T : KotlinGradleFragment> KotlinGradleFragmentConfigurationCapabilities<T>.onlyIfMadePublic(): KotlinGradleFragmentConfigurationCapabilities<T> {
    return KotlinGradleFragmentConfigurationCapabilities { fragment ->
        fragment.containingModule.ifMadePublic { setCapabilities(this@onlyIfMadePublic, fragment) }
    }
}

fun <T : KotlinGradleFragment> KotlinGradleFragmentConfigurationCapabilities.Context.setCapabilities(
    capabilities: KotlinGradleFragmentConfigurationCapabilities<T>, fragment: T
) = with(capabilities) { setCapabilities(fragment) }

operator fun <T : KotlinGradleFragment> FragmentCapabilities<T>.plus(other: FragmentCapabilities<T>): FragmentCapabilities<T> {
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
    override fun KotlinGradleFragmentConfigurationCapabilities.Context.setCapabilities(fragment: T) {
        children.forEach { capability -> setCapabilities(capability, fragment) }
    }
}
