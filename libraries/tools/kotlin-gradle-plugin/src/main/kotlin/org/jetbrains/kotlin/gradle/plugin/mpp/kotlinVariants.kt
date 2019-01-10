/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.ExcludeRule
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.component.ComponentWithCoordinates
import org.gradle.api.component.ComponentWithVariants
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetComponent
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.lowerSpinalCaseName

internal interface KotlinTargetComponentWithPublication : KotlinTargetComponent {
    // This property is declared in the separate parent type to allow the usages to reference it without forcing the subtypes to load,
    // which is needed for compatibility with older Gradle versions
    var publicationDelegate: MavenPublication?
}

internal fun getCoordinatesFromPublicationDelegateAndProject(
    publication: MavenPublication?,
    project: Project,
    target: KotlinTarget?
): ModuleVersionIdentifier =
    object : ModuleVersionIdentifier {
        private val moduleName: String
            get() =
                publication?.artifactId ?: lowerSpinalCaseName(project.name, target?.name)

        private val moduleGroup: String
            get() =
                publication?.groupId ?: project.group.toString()

        override fun getGroup() = moduleGroup
        override fun getName() = moduleName
        override fun getVersion() = publication?.version ?: project.version.toString()

        override fun getModule(): ModuleIdentifier = object : ModuleIdentifier {
            override fun getGroup(): String = moduleGroup
            override fun getName(): String = moduleName
        }
    }

private interface KotlinTargetComponentWithCoordinatesAndPublication :
    KotlinTargetComponentWithPublication,
    ComponentWithCoordinates /* Gradle 4.7+ API, don't use with older versions */
{
    override fun getCoordinates() = getCoordinatesFromPublicationDelegateAndProject(publicationDelegate, target.project, target)
}

open class KotlinVariant(
    val producingCompilation: KotlinCompilation<*>,
    private val usages: Set<DefaultKotlinUsageContext>
) : KotlinTargetComponentWithPublication, SoftwareComponentInternal {
    var componentName: String? = null

    final override val target: KotlinTarget
        get() = producingCompilation.target

    override fun getUsages(): Set<UsageContext> = usages

    override fun getName(): String = componentName ?: producingCompilation.target.targetName

    override val publishable: Boolean
        get() = target.publishable

    internal var defaultArtifactIdSuffix: String? = null

    override val defaultArtifactId: String
        get() = lowerSpinalCaseName(target.project.name, target.targetName, defaultArtifactIdSuffix)

    override var publicationDelegate: MavenPublication? = null
}

open class KotlinVariantWithCoordinates(
    producingCompilation: KotlinCompilation<*>,
    usages: Set<DefaultKotlinUsageContext>
) : KotlinVariant(producingCompilation, usages),
    KotlinTargetComponentWithCoordinatesAndPublication /* Gradle 4.7+ API, don't use with older versions */

class KotlinVariantWithMetadataVariant(
    producingCompilation: KotlinCompilation<*>,
    usages: Set<DefaultKotlinUsageContext>,
    private val metadataTarget: KotlinTarget
) : KotlinVariantWithCoordinates(producingCompilation, usages), ComponentWithVariants {
    override fun getVariants() = metadataTarget.components
}

class KotlinVariantWithMetadataDependency(
    producingCompilation: KotlinCompilation<*>,
    val originalUsages: Set<DefaultKotlinUsageContext>,
    private val metadataTarget: KotlinTarget
) : KotlinVariantWithCoordinates(producingCompilation, originalUsages) {
    override fun getUsages(): Set<UsageContext> = originalUsages.mapTo(mutableSetOf()) { usageContext ->
        KotlinUsageContextWithAdditionalDependencies(usageContext, setOf(metadataDependency()))
    }

    private fun metadataDependency(): ModuleDependency {
        val metadataPublication = (metadataTarget.components.single() as KotlinTargetComponentWithPublication).publicationDelegate!!
        val metadataGroupId = metadataPublication.groupId
        val metadataArtifactId = metadataPublication.artifactId
        val metadataVersion = metadataPublication.version
        return target.project.dependencies.module("$metadataGroupId:$metadataArtifactId:$metadataVersion") as ModuleDependency
    }

    class KotlinUsageContextWithAdditionalDependencies(
        val parentUsageContext: DefaultKotlinUsageContext,
        val additionalDependencies: Set<ModuleDependency>
    ) : KotlinUsageContext by parentUsageContext {
        override fun getDependencies() = parentUsageContext.dependencies + additionalDependencies

        override fun getGlobalExcludes(): Set<ExcludeRule> = emptySet()
    }
}

class JointAndroidKotlinTargetComponent(
    override val target: KotlinAndroidTarget,
    private val nestedVariants: Set<KotlinVariant>,
    val flavorNames: List<String>
) : KotlinTargetComponentWithCoordinatesAndPublication, SoftwareComponentInternal {

    override fun getUsages(): Set<UsageContext> = nestedVariants.flatMap { it.usages }.toSet()

    override fun getName(): String = lowerCamelCaseName(target.targetName, *flavorNames.toTypedArray())

    override val publishable: Boolean
        get() = target.publishable

    override val defaultArtifactId: String =
        lowerSpinalCaseName(target.project.name, target.targetName, *flavorNames.toTypedArray())

    override var publicationDelegate: MavenPublication? = null
}
