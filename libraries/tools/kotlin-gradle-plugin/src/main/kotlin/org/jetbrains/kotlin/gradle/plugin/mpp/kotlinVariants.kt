/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.*
import org.gradle.api.component.ComponentWithCoordinates
import org.gradle.api.component.ComponentWithVariants
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetComponent
import org.jetbrains.kotlin.gradle.utils.dashSeparatedName
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

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
                publication?.artifactId ?: dashSeparatedName(project.name, target?.name?.toLowerCase())

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

    override fun getUsages(): Set<KotlinUsageContext> = usages

    override fun getName(): String = componentName ?: producingCompilation.target.targetName

    override val publishable: Boolean
        get() = target.publishable

    override var sourcesArtifacts: Set<PublishArtifact> = emptySet()
        internal set

    internal var defaultArtifactIdSuffix: String? = null

    override val defaultArtifactId: String
        get() = dashSeparatedName(target.project.name, target.targetName.toLowerCase(), defaultArtifactIdSuffix)

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
    internal val metadataTarget: AbstractKotlinTarget
) : KotlinVariantWithCoordinates(producingCompilation, usages), ComponentWithVariants {
    override fun getVariants() = metadataTarget.components
}

class KotlinVariantWithMetadataDependency(
    producingCompilation: KotlinCompilation<*>,
    val originalUsages: Set<DefaultKotlinUsageContext>,
    private val metadataTarget: AbstractKotlinTarget
) : KotlinVariantWithCoordinates(producingCompilation, originalUsages) {
    override fun getUsages(): Set<KotlinUsageContext> = originalUsages.mapTo(mutableSetOf()) { usageContext ->
        KotlinUsageContextWithAdditionalDependencies(usageContext, setOf(metadataDependency()))
    }

    private fun metadataDependency(): ModuleDependency {
        val metadataComponent = metadataTarget.kotlinComponents.single() as KotlinTargetComponentWithPublication
        val project = metadataTarget.project

        // The metadata component may not be published, e.g. if the whole project is not published:
        val metadataPublication: MavenPublication? = metadataComponent.publicationDelegate

        val metadataGroupId = metadataPublication?.groupId ?: project.group
        val metadataArtifactId = metadataPublication?.artifactId ?: metadataComponent.defaultArtifactId
        val metadataVersion = metadataPublication?.version ?: project.version
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
    val flavorNames: List<String>,
    override val sourcesArtifacts: Set<PublishArtifact>
    ) : KotlinTargetComponentWithCoordinatesAndPublication, SoftwareComponentInternal {

    override fun getUsages(): Set<KotlinUsageContext> = nestedVariants.flatMap { it.usages }.toSet()

    override fun getName(): String = lowerCamelCaseName(target.targetName, *flavorNames.toTypedArray())

    override val publishable: Boolean
        get() = target.publishable

    override val defaultArtifactId: String =
        dashSeparatedName(
            target.project.name,
            target.targetName.toLowerCase(),
            *flavorNames.map { it.toLowerCase() }.toTypedArray()
        )

    override var publicationDelegate: MavenPublication? = null
}
