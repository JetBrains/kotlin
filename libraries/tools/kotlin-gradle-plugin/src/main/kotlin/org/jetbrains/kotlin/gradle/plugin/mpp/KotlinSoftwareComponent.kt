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

internal class KotlinPlatformSoftwareComponent(
    private val project: Project,
    private val kotlinTargets: List<KotlinTarget>
) : SoftwareComponentInternal {
    override fun getUsages(): Set<UsageContext> = kotlinTargets.map(::KotlinPlatformUsageContext).toSet()

    override fun getName(): String = project.name

    companion object {
        val kotlinUsage = object : Usage {
            override fun getName(): String = "java-api" // TODO maybe it's better to have a Kotlin-specific usage here
        }
    }

    private inner class KotlinPlatformUsageContext(val kotlinTarget: KotlinTarget) : UsageContext {
        override fun getUsage(): Usage = kotlinUsage

        override fun getName(): String = kotlinTarget.targetName

        private val configuration: Configuration
            get() = project.configurations.getByName(kotlinTarget.runtimeConfigurationName)

        override fun getDependencies(): MutableSet<out ModuleDependency> =
            configuration.incoming.dependencies.withType(ModuleDependency::class.java)

        override fun getDependencyConstraints(): MutableSet<out DependencyConstraint> =
            configuration.incoming.dependencyConstraints

        override fun getArtifacts(): MutableSet<out PublishArtifact> =
            // TODO Gradle does that in a different way; check whether we can improve this
            configuration.artifacts

        override fun getAttributes(): AttributeContainer =
            configuration.outgoing.attributes

        // FIXME this is a stub for a function that is not present in the Gradle API that we compile against
        override fun getCapabilities(): Set<Capability> = emptySet()

        // FIXME this is a stub for a function that is not present in the Gradle API that we compile against
        fun getGlobalExcludes(): Set<Any> = emptySet()
    }
}