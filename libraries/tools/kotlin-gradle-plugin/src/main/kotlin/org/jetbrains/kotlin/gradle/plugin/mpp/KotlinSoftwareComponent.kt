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

    companion object {
        fun kotlinApiUsage(project: Project) = project.usageByName(Usage.JAVA_API)
        fun kotlinRuntimeUsage(project: Project) = project.usageByName(Usage.JAVA_RUNTIME)
    }
}

open class KotlinVariant(
    override val target: KotlinTarget
) : SoftwareComponentInternal, KotlinTargetComponent {
    override fun getUsages(): Set<UsageContext> = target.createUsageContexts()
    override fun getName(): String = target.name

    // This property is declared in the parent class to allow usages to reference it without forcing the subclass to load,
    // which is needed for compatibility with older Gradle versions
    internal var publicationDelegate: MavenPublication? = null

    override val publishable: Boolean
        get() = target.publishable
}

class KotlinVariantWithCoordinates(
    target: KotlinTarget
) : KotlinVariant(target),
    ComponentWithCoordinates /* Gradle 4.7+ API, don't use with older versions */
{
    override fun getCoordinates() = object : ModuleVersionIdentifier {
        private val project get() = target.project

        private val moduleName: String get() =
            publicationDelegate?.artifactId ?:
            "${project.name}-${target.name.toLowerCase()}"

        private val moduleGroup: String get() =
            publicationDelegate?.groupId ?:
            project.group.toString()

        override fun getGroup() = moduleGroup
        override fun getName() = moduleName
        override fun getVersion() = publicationDelegate?.version ?: project.version.toString()

        override fun getModule(): ModuleIdentifier = object : ModuleIdentifier {
            override fun getGroup(): String = moduleGroup
            override fun getName(): String = moduleName
        }
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
