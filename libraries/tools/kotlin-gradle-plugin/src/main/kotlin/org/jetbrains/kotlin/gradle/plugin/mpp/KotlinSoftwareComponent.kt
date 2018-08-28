/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyConstraint
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Usage
import org.gradle.api.capabilities.Capability
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.UsageContext
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.usageByName

class KotlinSoftwareComponent(
    private val project: Project,
    private val name: String,
    private val kotlinTargets: Iterable<KotlinTarget>
) : SoftwareComponentInternal {
    override fun getUsages(): Set<UsageContext> = kotlinTargets.flatMap { it.createUsageContexts() }.toSet()

    override fun getName(): String = name

    companion object {
        fun kotlinApiUsage(project: Project) = project.usageByName(Usage.JAVA_API)
        fun kotlinRuntimeUsage(project: Project) = project.usageByName(Usage.JAVA_RUNTIME)
    }
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

    override fun getName(): String = kotlinTarget.targetName + when (usage.name) {
        Usage.JAVA_API -> "-api"
        Usage.JAVA_RUNTIME -> "-runtime"
        else -> error("unexpected usage")
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