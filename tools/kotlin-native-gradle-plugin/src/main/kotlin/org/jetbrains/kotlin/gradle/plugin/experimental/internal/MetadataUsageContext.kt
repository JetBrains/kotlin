package org.jetbrains.kotlin.gradle.plugin.experimental.internal

import org.gradle.api.Named
import org.gradle.api.artifacts.*
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Usage
import org.gradle.api.capabilities.Capability
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.model.ObjectFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages

class MetadataUsageContext(
    private val _name: String,
    val objects: ObjectFactory,
    val configuration: Configuration
): UsageContext, Named {

    override fun getUsage(): Usage = objects.named(Usage::class.java, KotlinUsages.KOTLIN_API)
    override fun getName(): String = _name

    override fun getCapabilities(): Set<Capability> = emptySet()
    override fun getGlobalExcludes(): Set<ExcludeRule> = emptySet()

    override fun getDependencies(): Set<out ModuleDependency> =
        configuration.incoming.dependencies.withType(ModuleDependency::class.java)

    override fun getDependencyConstraints(): MutableSet<out DependencyConstraint> =
        configuration.incoming.dependencyConstraints

    override fun getArtifacts(): MutableSet<out PublishArtifact> =
        configuration.outgoing.artifacts

    override fun getAttributes(): AttributeContainer =
        configuration.outgoing.attributes
}
