/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.*
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Usage
import org.gradle.api.capabilities.Capability
import org.gradle.api.component.ComponentWithCoordinates
import org.gradle.api.component.ComponentWithVariants
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetComponent
import org.jetbrains.kotlin.gradle.plugin.usageByName

class KotlinSoftwareComponent(
    private val project: Project,
    private val name: String,
    private val kotlinTargets: Iterable<KotlinTarget>
) : SoftwareComponentInternal, ComponentWithVariants {
    
    override fun getUsages(): Set<UsageContext> = emptySet()

    override fun getVariants(): Set<KotlinTargetComponent> =
        kotlinTargets.map { it.component }.toSet()

    override fun getName(): String = name
}

// At the moment all KN artifacts have JAVA_API usage.
// TODO: Replace it with a specific usage
object NativeUsage {
    const val KOTLIN_KLIB = "kotlin-klib"
}

internal class KotlinPlatformUsageContext(
    val project: Project,
    val kotlinTarget: KotlinTarget,
    private val usage: Usage,
    val dependencyConfigurationName: String
) : UsageContext {
    override fun getUsage(): Usage = usage

    override fun getName(): String = kotlinTarget.targetName + when (dependencyConfigurationName) {
        kotlinTarget.apiElementsConfigurationName -> "-api"
        kotlinTarget.runtimeElementsConfigurationName -> "-runtime"
        else -> error("unexpected configuration")
    }

    private val configuration: Configuration
        get() = project.configurations.getByName(dependencyConfigurationName)

    override fun getDependencies(): MutableSet<out ModuleDependency> =
        configuration.incoming.dependencies.withType(ModuleDependency::class.java)

    override fun getDependencyConstraints(): MutableSet<out DependencyConstraint> =
        configuration.incoming.dependencyConstraints

    override fun getArtifacts(): MutableSet<out PublishArtifact> =
    // TODO Gradle Java plugin does that in a different way; check whether we can improve this
        configuration.artifacts

    override fun getAttributes(): AttributeContainer =
        configuration.outgoing.attributes

    override fun getCapabilities(): Set<Capability> = emptySet()

    // FIXME this is a stub for a function that is not present in the Gradle API that we compile against
    fun getGlobalExcludes(): Set<Any> = emptySet()
}
