/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.*
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.capabilities.Capability
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.component.ComponentWithCoordinates
import org.gradle.api.component.ComponentWithVariants
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.provider.Provider
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.metadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.attributes.KlibPackaging
import org.jetbrains.kotlin.gradle.plugin.mpp.archive.KotlinTargetWithKotlinArchiveSupport
import org.jetbrains.kotlin.gradle.plugin.mpp.archive.defaultKotlinUsageContextMaybeReplacedWithKar
import org.jetbrains.kotlin.gradle.plugin.mpp.publishing.kotlinMultiplatformRootPublication
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.KmpPublicationStrategy
import org.jetbrains.kotlin.gradle.plugin.sources.defaultImpl
import org.jetbrains.kotlin.gradle.targets.metadata.awaitMetadataCompilationsCreated
import org.jetbrains.kotlin.gradle.targets.metadata.getCommonSourceSetsForMetadataCompilation
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

abstract class KotlinSoftwareComponent(
    private val project: Project,
    private val name: String,
    protected val kotlinTargets: Iterable<KotlinTarget>,
    private val includeExtraUsagesFrom: AdhocComponentWithVariants,
) : SoftwareComponentInternal, ComponentWithVariants {

    override fun getName(): String = name


    internal val uklibUsages: CompletableFuture<List<DefaultKotlinUsageContext>> = CompletableFuture()

    private fun KotlinTarget.publishAsAvailableAtPointers(): Boolean {
        return when (this) {
            is KotlinMetadataTarget -> false
            is KotlinTargetWithKotlinArchiveSupport -> !isStoredInKotlinArchive.get()
            else -> true
        }
    }

    private fun KotlinTarget.publishableSoftwareComponents(): List<SoftwareComponent> {
        val targetPublishableComponentNames = internal.kotlinComponents
            .filter { component -> component.publishable }
            .map { component -> component.name }
            .toSet()

        return components.filter { it.name in targetPublishableComponentNames }
    }

    /**
     * These are variants pointing to subcomponent variants via available-at pointers
     */
    override fun getVariants(): Set<SoftwareComponent> {
        return kotlinTargets
            .filter { target -> target.publishAsAvailableAtPointers() }
            .flatMap { target -> target.publishableSoftwareComponents() }
            .toSet()
    }

    /**
     * This component stores usages related to metadata target.
     *
     * As implementation detail, it reuses kotlinTargetSoftwareComponent, as it
     * already correctly handles creating -published variants, which is
     * required to handle replacement of metadata jar with kotlin archive.
     */
    private val rootSoftwareComponentDelegate: Future<SoftwareComponent> = project.future {
        val metadataTarget = project.multiplatformExtension.metadataTarget
        val mainCompilation = metadataTarget.awaitMetadataCompilationsCreated().getByName(MAIN_COMPILATION_NAME)
        val usages = buildSet {
            add(
                project.defaultKotlinUsageContextMaybeReplacedWithKar(
                    isStoredInKotlinArchive = project.multiplatformExtension.publishing.publicationFormat.map { it == KotlinPublicationFormat.KOTLIN_ARCHIVE },
                    compilation = mainCompilation,
                    mavenScope = KotlinUsageContext.MavenScope.COMPILE,
                    dependencyConfigurationName = metadataTarget.apiElementsConfigurationName,
                )
            )

            val sourcesElements = metadataTarget.sourcesElementsConfigurationName
            if (metadataTarget.isSourcesPublishable) {
                addSourcesJarArtifactToConfiguration(
                    sourcesElements,
                    classifierPrefix = when (project.kotlinPropertiesProvider.kmpPublicationStrategy) {
                        KmpPublicationStrategy.UklibPublicationInASingleComponentWithKMPPublication -> "metadata"
                        KmpPublicationStrategy.StandardKMPPublication -> null
                    },
                )
                add(
                    DefaultKotlinUsageContext(
                        compilation = mainCompilation,
                        dependencyConfigurationName = sourcesElements,
                        includeIntoProjectStructureMetadata = false,
                        publishOnlyIf = { metadataTarget.isSourcesPublishable }
                    )
                )
            }
        }
        KotlinTargetSoftwareComponent(metadataTarget,KotlinVariant(mainCompilation, usages))
    }


    override fun getUsages(): Set<UsageContext> {
        return buildSet {
            val embeddedPlatformComponents = kotlinTargets
                .filterNot { target -> target.publishAsAvailableAtPointers() }
                .flatMap { target -> target.publishableSoftwareComponents() }
            val allPublishedPlatformComponents = embeddedPlatformComponents + listOf(
                rootSoftwareComponentDelegate.getOrThrow(),
                includeExtraUsagesFrom
            )
            for (component in allPublishedPlatformComponents) {
                addAll((component as SoftwareComponentInternal).usages)
            }
            addAll(uklibUsages.getOrThrow())
        }
    }

    private suspend fun allPublishableCommonSourceSets() = getCommonSourceSetsForMetadataCompilation(project) +
            getHostSpecificMainSharedSourceSets(project)

    /**
     * Registration (during object init) of [sourcesJarTask] is required for cases when
     * user build scripts want to have access to sourcesJar task to configure it
     */
    private val sourcesJarTask: TaskProvider<Jar> = sourcesJarTaskNamed(
        "sourcesJar",
        name,
        project,
        project.future { allPublishableCommonSourceSets().associate { it.name to it.defaultImpl.allKotlin } },
        name.toLowerCaseAsciiOnly()
    )

    private fun addSourcesJarArtifactToConfiguration(
        configurationName: String,
        classifierPrefix: String?,
    ): PublishArtifact {
        return project.artifacts.add(configurationName, sourcesJarTask) { sourcesJarArtifact ->
            sourcesJarArtifact.classifier = dashSeparatedName(
                listOfNotNull(
                    classifierPrefix,
                    "sources",
                )
            )
        }
    }

    val publicationDelegate: MavenPublication? get() = project.kotlinMultiplatformRootPublication.lenient.getOrNull()
}

class KotlinSoftwareComponentWithCoordinatesAndPublication
@InternalKotlinGradlePluginApi
constructor(
    project: Project,
    name: String,
    kotlinTargets: Iterable<KotlinTarget>,
    includeExtraUsagesFrom: AdhocComponentWithVariants,
) : KotlinSoftwareComponent(project, name, kotlinTargets, includeExtraUsagesFrom), ComponentWithCoordinates {

    override fun getCoordinates(): ModuleVersionIdentifier = getCoordinatesFromPublicationDelegateAndProject(
        publicationDelegate, kotlinTargets.first().project, null
    )
}

interface KotlinUsageContext : UsageContext {
    val compilation: KotlinCompilation<*>
    val dependencyConfigurationName: String
    val includeIntoProjectStructureMetadata: Boolean
    val mavenScope: MavenScope?

    fun configurePublishedConfiguration(configuration: Configuration, targetComponent: KotlinTargetComponent) {}

    enum class MavenScope {
        COMPILE, RUNTIME;
    }
}

class DefaultKotlinUsageContext(
    override val compilation: KotlinCompilation<*>,
    override val mavenScope: KotlinUsageContext.MavenScope? = null,
    override val dependencyConfigurationName: String,
    internal val overrideConfigurationArtifacts: Provider<Set<PublishArtifact>>? = null,
    internal val overrideConfigurationAttributes: AttributeContainer? = null,
    override val includeIntoProjectStructureMetadata: Boolean = true,
    internal val publishOnlyIf: PublishOnlyIf = PublishOnlyIf { true },
    private val configurePublishedConfiguration: Configuration.(KotlinTargetComponent) -> Unit = {},
) : KotlinUsageContext {
    fun interface PublishOnlyIf {
        fun predicate(): Boolean
    }

    private val kotlinTarget: KotlinTarget get() = compilation.target
    private val project: Project get() = kotlinTarget.project

    override fun getName(): String = dependencyConfigurationName

    private val configuration: Configuration
        get() = project.configurations.getByName(dependencyConfigurationName)

    override fun getDependencies(): MutableSet<out ModuleDependency> =
        configuration.incoming.dependencies.withType(ModuleDependency::class.java)

    override fun getDependencyConstraints(): MutableSet<out DependencyConstraint> =
        configuration.incoming.dependencyConstraints

    override fun getArtifacts(): Set<PublishArtifact> =
        overrideConfigurationArtifacts?.orNull?.toSet() ?:
        // TODO Gradle Java plugin does that in a different way; check whether we can improve this
        configuration.artifacts

    override fun getAttributes(): AttributeContainer {
        val configurationAttributes = overrideConfigurationAttributes ?: configuration.attributes

        /** TODO Using attributes of a detached configuration is a small and 'conservative' fix for KT-29758, [HierarchyAttributeContainer]
         * being rejected by Gradle 5.2+; we may need to either not filter the attributes, which will lead to
         * [ProjectLocalConfigurations.ATTRIBUTE] being published in the Gradle module metadata, which will potentially complicate our
         * attributes schema migration, or create proper, non-detached configurations for publishing that are separated from the
         * configurations used for project-to-project dependencies
         */
        val result = project.configurations.detachedResolvable().attributes

        configurationAttributes.copyAttributesTo(
            project.providers,
            dest = result,
            keys = filterOutNonPublishableAttributes(configurationAttributes.keySet())
        )

        return result
    }

    override fun getCapabilities(): Set<Capability> = emptySet()

    override fun getGlobalExcludes(): Set<ExcludeRule> = emptySet()

    private val publishJvmEnvironmentAttribute get() = project.kotlinPropertiesProvider.publishJvmEnvironmentAttribute

    private fun filterOutNonPublishableAttributes(attributes: Set<Attribute<*>>): Set<Attribute<*>> =
        attributes.filterTo(mutableSetOf()) {
            it != ProjectLocalConfigurations.ATTRIBUTE &&
                    /**
                     * We exclude the attribute "org.gradle.jvm.environment" from publishing to avoid two issues:
                     *
                     * 1. Kotlin < 1.6.0 consumers which don't set this attribute on the consumer side. If this attribute is not set on the
                     * consumer side, then the Gradle built-in disambiguation rule applies: { standard-jvm, android } -> standard-jvm.
                     * In Kotlin 1.5.31, this would conflict with the rule on o.j.k.platform.type: { androidJvm, jvm } -> androidJvm, so the
                     * two rules would choose different closes match variants, and disambiguation would fail.
                     *
                     * 2. If this attribute is published, but not present on all the variants in a multiplatform library, and is also
                     * missing on the consumer side (like Gradle < 7.0, Kotlin 1.6.0), then there is a
                     * case when Gradle fails to choose a variant in a completely reasonable setup.
                     *
                     * UPD: 1.9.20:
                     * We should now be ready to publish the 'jvm environment' attribute.
                     * It will however be rolled out as 'opt-in' first (as safety measure).
                     * We expect that the 'external Android target' will opt-into publishing this attribute,
                     * as it will switch to KotlinPlatformType.jvm and requires this additional attribute to disambiguate
                     * Android from the JVM
                     */
                    (it.name != "org.gradle.jvm.environment" || publishJvmEnvironmentAttribute) &&
                    /**
                     * Non-packed klibs are used only locally and should not be published.
                     * Thus, it does not make sense to publish this attribute as well.
                     *
                     * Another option could be to put this attribute only on the secondary variant that is non-packed.
                     * However, disambiguation rules do not work well on old Gradle versions with this.
                     */
                    it.name != KlibPackaging.ATTRIBUTE.name
        }

    override fun configurePublishedConfiguration(configuration: Configuration, targetComponent: KotlinTargetComponent) {
        configuration.configurePublishedConfiguration(targetComponent)
    }

}

internal fun Iterable<DefaultKotlinUsageContext>.publishableUsages() = this
    .filter { it.publishOnlyIf.predicate() }
    .toSet()
