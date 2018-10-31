/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.artifacts.ExcludeRule
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.component.ComponentWithCoordinates
import org.gradle.api.component.ComponentWithVariants
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetComponent

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

open class KotlinVariantWithCoordinates(
    target: KotlinTarget
) : KotlinVariant(target),
    ComponentWithCoordinates /* Gradle 4.7+ API, don't use with older versions */ {
    override fun getCoordinates() = object : ModuleVersionIdentifier {
        private val project get() = target.project

        private val moduleName: String
            get() =
                publicationDelegate?.artifactId ?: "${project.name}-${target.name.toLowerCase()}"

        private val moduleGroup: String
            get() =
                publicationDelegate?.groupId ?: project.group.toString()

        override fun getGroup() = moduleGroup
        override fun getName() = moduleName
        override fun getVersion() = publicationDelegate?.version ?: project.version.toString()

        override fun getModule(): ModuleIdentifier = object : ModuleIdentifier {
            override fun getGroup(): String = moduleGroup
            override fun getName(): String = moduleName
        }
    }
}

class KotlinVariantWithMetadataVariant(target: KotlinTarget, private val metadataTarget: KotlinTarget) :
    KotlinVariantWithCoordinates(target), ComponentWithVariants {
    override fun getVariants() = setOf(metadataTarget.component)
}

class KotlinVariantWithMetadataDependency(target: KotlinTarget, private val metadataTarget: KotlinTarget) :
    KotlinVariantWithCoordinates(target) {
    override fun getUsages(): Set<UsageContext> = target.createUsageContexts().mapTo(mutableSetOf()) { usageContext ->
        UsageContextWithAdditionalDependencies(usageContext, setOf(metadataDependency()))
    }

    private fun metadataDependency(): ModuleDependency {
        val metadataPublication = (metadataTarget.component as KotlinVariant).publicationDelegate!!
        val metadataGroupId = metadataPublication.groupId
        val metadataArtifactId = metadataPublication.artifactId
        val metadataVersion = metadataPublication.version
        return target.project.dependencies.module("$metadataGroupId:$metadataArtifactId:$metadataVersion") as ModuleDependency
    }

    private class UsageContextWithAdditionalDependencies(
        val parentUsageContext: UsageContext,
        val additionalDependencies: Set<ModuleDependency>
    ) : UsageContext by parentUsageContext {
        override fun getDependencies() = parentUsageContext.dependencies + additionalDependencies

        override fun getGlobalExcludes(): Set<ExcludeRule> = emptySet()
    }
}
