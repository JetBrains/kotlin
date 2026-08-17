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
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.provider.Provider
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.metadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.AfterFinaliseCompilations
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.attributes.KlibPackaging
import org.jetbrains.kotlin.gradle.plugin.mpp.archive.KotlinTargetWithKotlinArchiveSupport
import org.jetbrains.kotlin.gradle.plugin.mpp.publishing.kotlinMultiplatformRootPublication
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.KmpPublicationStrategy
import org.jetbrains.kotlin.gradle.plugin.sources.defaultImpl
import org.jetbrains.kotlin.gradle.targets.metadata.getCommonSourceSetsForMetadataCompilation
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

abstract class KotlinSoftwareComponent(
    private val project: Project,
    protected val kotlinTargets: Iterable<KotlinTarget>,
    private val includeExtraUsagesFrom: AdhocComponentWithVariants,
) : SoftwareComponentInternal, ComponentWithVariants, ComponentWithCoordinates {

    private val adhocDelegate = (project as ProjectInternal).services
        .get(SoftwareComponentFactory::class.java)
        .adhoc("kotlin")

    override fun getName(): String = "kotlin"

    private val metadataTarget get() = project.multiplatformExtension.metadataTarget

    internal val uklibUsages: CompletableFuture<List<DefaultKotlinUsageContext>> = CompletableFuture()

    private fun KotlinTarget.publishAsAvailableAtPointers(): Boolean {
        return when (this) {
            is KotlinMetadataTarget -> false
            is KotlinTargetWithKotlinArchiveSupport -> isPublishedInSeparateComponent.get()
            else -> true
        }
    }

    /**
     * These are variants pointing to subcomponent variants via available-at pointers
     */
    private val _variants: Future<Set<SoftwareComponent>> = project.future {
        AfterFinaliseCompilations.await()

        kotlinTargets
            .filter { target -> target.publishAsAvailableAtPointers() }
            .flatMap { target ->
                val targetPublishableComponentNames = target.internal.kotlinComponents
                    .filter { component -> component.publishable }
                    .map { component -> component.name }
                    .toSet()

                target.components.filter { it.name in targetPublishableComponentNames }
            }.toSet()
    }

    override fun getVariants(): Set<SoftwareComponent> = _variants.getOrThrow()

    /**
     * These are variants exposed directly through the root component
     */
    private val _usages: Future<Unit> = project.future {
        AfterFinaliseCompilations.await()

        val usages = buildList {
            val mainCompilation = metadataTarget.compilations.getByName(MAIN_COMPILATION_NAME)
            this += project.defaultKotlinUsageContextMaybeReplacedWithKar(
                isStoredInKotlinArchive = multiplatformExtension.publishing.publicationFormat.map { it == KotlinPublicationFormat.KOTLIN_ARCHIVE },
                requiresPlatformComponentCompatibilityCapability = null,
                compilation = mainCompilation,
                mavenScope = KotlinUsageContext.MavenScope.COMPILE,
                dependencyConfigurationName = metadataTarget.apiElementsConfigurationName,
                componentName = null,
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
                this += DefaultKotlinUsageContext(
                    compilation = mainCompilation,
                    dependencyConfigurationName = sourcesElements,
                    includeIntoProjectStructureMetadata = false,
                    publishOnlyIf = { metadataTarget.isSourcesPublishable }
                )
            }
            kotlinTargets
                .asSequence()
                .filterNot { target -> target.publishAsAvailableAtPointers() }
                .flatMap { target -> target.internal.kotlinComponents }
                .filter { component -> component.publishable }
                .filterIsInstance<KotlinVariant>()
                .flatMapTo(this) { variant -> variant.usages }
        }

        usages.forEach { addVariantsFromKotlinUsage(it) }
    }

    fun addVariantsFromKotlinUsage(kotlinUsageContext: KotlinUsageContext) {
        project.publishConfiguration(adhocDelegate, kotlinUsageContext)
    }


    override fun getUsages(): Set<UsageContext> {
        _usages.getOrThrow()
        return buildSet {
            addAll((includeExtraUsagesFrom as SoftwareComponentInternal).usages)
            addAll((adhocDelegate as SoftwareComponentInternal).usages)
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
    kotlinTargets: Iterable<KotlinTarget>,
    includeExtraUsagesFrom: AdhocComponentWithVariants,
) : KotlinSoftwareComponent(project, kotlinTargets, includeExtraUsagesFrom), ComponentWithCoordinates {

    override fun getCoordinates(): ModuleVersionIdentifier = getCoordinatesFromPublicationDelegateAndProject(
        publicationDelegate, kotlinTargets.first().project, null
    )
}

interface KotlinUsageContext : UsageContext {
    val compilation: KotlinCompilation<*>
    val dependencyConfigurationName: String
    val includeIntoProjectStructureMetadata: Boolean
    val mavenScope: MavenScope?

    suspend fun configurePublishedConfiguration(configuration: Configuration) {}

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
    private val configurePublishedConfiguration: suspend Configuration.() -> Unit = {},
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

    override suspend fun configurePublishedConfiguration(configuration: Configuration) {
        configuration.configurePublishedConfiguration()
    }

}

internal fun Iterable<DefaultKotlinUsageContext>.publishableUsages() = this
    .filter { it.publishOnlyIf.predicate() }
    .toSet()
