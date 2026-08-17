/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Action
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier
import org.gradle.api.provider.Provider
import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsageContext.MavenScope.COMPILE
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsageContext.MavenScope.RUNTIME
import org.jetbrains.kotlin.gradle.plugin.mpp.archive.KotlinTargetWithKotlinArchiveSupport
import org.jetbrains.kotlin.gradle.plugin.mpp.archive.addOutgoingKarArtifactTo
import org.jetbrains.kotlin.gradle.plugin.mpp.archive.karPackTask

import org.jetbrains.kotlin.gradle.targets.android.internal.InternalKotlinTargetPreset
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.tooling.core.MutableExtras
import org.jetbrains.kotlin.tooling.core.mutableExtrasOf
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.jetbrains.kotlin.utils.addIfNotNull

internal const val PRIMARY_SINGLE_COMPONENT_NAME = "kotlin"

@InternalKotlinGradlePluginApi
abstract class AbstractKotlinTarget(
    final override val project: Project,
) : InternalKotlinTarget {

    final override val extras: MutableExtras = mutableExtrasOf()

    private val attributeContainer = HierarchyAttributeContainer(parent = null, project.objects)

    override fun getAttributes(): AttributeContainer = attributeContainer

    override val apiElementsConfigurationName: String
        get() = disambiguateName("apiElements")

    override val runtimeElementsConfigurationName: String
        get() = disambiguateName("runtimeElements")

    override val sourcesElementsConfigurationName: String
        get() = disambiguateName("sourcesElements")

    @InternalKotlinGradlePluginApi
    override val resourcesElementsConfigurationName: String
        get() = disambiguateName("resourcesElements")

    override val artifactsTaskName: String
        get() = disambiguateName("jar")

    override fun toString(): String = "target $name ($platformType)"

    override val publishable: Boolean
        get() = true

    override var isSourcesPublishable: Boolean = true
    override fun withSourcesJar(publish: Boolean) {
        isSourcesPublishable = publish
    }

    @InternalKotlinGradlePluginApi
    override val kotlinComponents: Set<KotlinTargetComponent> by lazy {
        val componentName =
            if (project.kotlinExtension is KotlinMultiplatformExtension)
                targetName
            else PRIMARY_SINGLE_COMPONENT_NAME

        val mainCompilation = compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
        val usageContexts = createPlatformCompilationsUsageContexts(componentName, mainCompilation).toMutableSet()

        usageContexts.addIfNotNull(
            createSourcesJarAndUsageContextIfPublishable(
                producingCompilation = mainCompilation,
                componentName = componentName,
                artifactNameAppendix = dashSeparatedName(targetName.toLowerCaseAsciiOnly())
            )
        )

        val result = createKotlinVariant(componentName, mainCompilation, usageContexts)

        setOf(result)
    }


    /**
     * Returns, potentially not configured (e.g. without some usages), Gradle SoftwareComponent's for this target
     */
    override val components: Set<KotlinTargetSoftwareComponent> by lazy {
        // TODO: Defer this read until after the DSL is finalised when this provider becomes user-configurable.
        if (this is KotlinTargetWithKotlinArchiveSupport && !isPublishedInSeparateComponent.get()) {
            emptySet()
        } else {
            kotlinComponents.map { kotlinComponent -> KotlinTargetSoftwareComponent(this, kotlinComponent) }.toSet()
        }
    }

    protected open fun createKotlinVariant(
        componentName: String,
        compilation: KotlinCompilation<*>,
        usageContexts: Set<DefaultKotlinUsageContext>
    ): KotlinVariant {
        val kotlinExtension = project.kotlinExtension

        val result =
            if (kotlinExtension !is KotlinMultiplatformExtension || targetName == KotlinMetadataTarget.METADATA_TARGET_NAME)
                KotlinVariantWithCoordinates(compilation, usageContexts)
            else {
                val metadataTarget =
                    kotlinExtension.targets.getByName(KotlinMetadataTarget.METADATA_TARGET_NAME) as AbstractKotlinTarget

                KotlinVariantWithMetadataVariant(compilation, usageContexts, metadataTarget)
            }

        result.componentName = componentName
        return result
    }

    internal open fun createPlatformCompilationsUsageContexts(
        componentName: String,
        producingCompilation: KotlinCompilation<*>
    ): Set<DefaultKotlinUsageContext> {
        return listOfNotNull(
            COMPILE to apiElementsConfigurationName,
            (RUNTIME to runtimeElementsConfigurationName).takeIf {
                @Suppress("DEPRECATION_ERROR")
                producingCompilation is KotlinCompilationToRunnableFiles
            }
        ).mapTo(mutableSetOf()) { (mavenScope, dependenciesConfigurationName) ->
            project.defaultKotlinUsageContextMaybeReplacedWithKar(
                isStoredInKotlinArchive = if (this is KotlinTargetWithKotlinArchiveSupport) { isStoredInKotlinArchive } else null,
                requiresPlatformComponentCompatibilityCapability = if (this is KotlinTargetWithKotlinArchiveSupport) { requiresPlatformComponentCompatibilityCapability } else null,
                compilation = producingCompilation,
                mavenScope = mavenScope,
                dependencyConfigurationName = dependenciesConfigurationName,
                componentName = componentName,
            )
        }
    }

    protected fun createSourcesJarAndUsageContextIfPublishable(
        producingCompilation: KotlinCompilation<*>,
        componentName: String,
        artifactNameAppendix: String,
        classifierPrefix: String? = null,
        sourcesElementsConfigurationName: String = this.sourcesElementsConfigurationName,
        overrideConfigurationAttributes: AttributeContainer? = null,
        mavenScope: KotlinUsageContext.MavenScope? = null,
    ): DefaultKotlinUsageContext? {
        // We want to create task anyway, even if sources are not going to be published by KGP
        // So users or other plugins can still use it
        val sourcesJarTask = sourcesJarTask(producingCompilation, componentName, artifactNameAppendix)
        if (!isSourcesPublishable) return null

        // If sourcesElements configuration not found, don't create artifact.
        // This can happen in pure JVM plugin where source publication is delegated to Java Gradle Plugin.
        // But we still want to have sourcesJarTask be registered
        project.configurations.findByName(sourcesElementsConfigurationName) ?: return null

        val artifact = project.artifacts.add(sourcesElementsConfigurationName, sourcesJarTask) as ConfigurablePublishArtifact
        artifact.classifier = dashSeparatedName(classifierPrefix, "sources")

        return DefaultKotlinUsageContext(
            compilation = producingCompilation,
            dependencyConfigurationName = sourcesElementsConfigurationName,
            overrideConfigurationAttributes = overrideConfigurationAttributes,
            mavenScope = mavenScope,
            includeIntoProjectStructureMetadata = false,
            publishOnlyIf = {
                if (this is KotlinTargetWithKotlinArchiveSupport) {
                    isSourcesPublishable && isPublishedInSeparateComponent.get()
                } else {
                    isSourcesPublishable
                }
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private val publicationConfigureActions: DomainObjectSet<Action<MavenPublication>> = project.objects
        .domainObjectSet(Action::class.java) as DomainObjectSet<Action<MavenPublication>>

    override fun mavenPublication(action: Action<MavenPublication>) {
        publicationConfigureActions.add(action)
    }

    @InternalKotlinGradlePluginApi
    override fun onPublicationCreated(publication: MavenPublication) {
        publicationConfigureActions.all { action -> action.execute(publication) }
    }

    override var targetPreset: InternalKotlinTargetPreset<KotlinTarget>? = null
        internal set
}

internal fun KotlinTarget.disambiguateName(simpleName: String) =
    lowerCamelCaseName(targetName, simpleName)

internal fun Project.defaultKotlinUsageContextMaybeReplacedWithKar(
    isStoredInKotlinArchive: Provider<Boolean>?,
    requiresPlatformComponentCompatibilityCapability: Provider<Boolean>?,
    compilation: KotlinCompilation<*>,
    mavenScope: KotlinUsageContext.MavenScope?,
    dependencyConfigurationName: String,
    componentName: String?,
    includeIntoProjectStructureMetadata: Boolean = true,
    publishOnlyIf: DefaultKotlinUsageContext.PublishOnlyIf = DefaultKotlinUsageContext.PublishOnlyIf { true },
): DefaultKotlinUsageContext {
    val platformComponentCapabilityIds: Provider<Set<ModuleVersionIdentifier>>? = requiresPlatformComponentCompatibilityCapability?.map {
        if (it) {
            require(componentName != null)
            val rootCoordinates = multiplatformExtension.rootSoftwareComponent.coordinates
            setOf(
                rootCoordinates,
                DefaultModuleVersionIdentifier.newId(
                    /* group = */ rootCoordinates.group,
                    /* name = */ dashSeparatedName(rootCoordinates.name, componentName.toLowerCaseAsciiOnly()),
                    /* version = */ rootCoordinates.version
                ),
            )
        } else {
            null
        }
    }
    val overrideConfigurationArtifacts: Provider<Set<PublishArtifact>>? = isStoredInKotlinArchive?.map { if (it) emptySet<PublishArtifact>() else null }
    val karTaskProvider = karPackTask
    return DefaultKotlinUsageContext(
        compilation = compilation,
        mavenScope = mavenScope,
        dependencyConfigurationName = dependencyConfigurationName,
        includeIntoProjectStructureMetadata = includeIntoProjectStructureMetadata,
        publishOnlyIf = publishOnlyIf,
        overrideConfigurationArtifacts = overrideConfigurationArtifacts,
        configurePublishedConfiguration = {
            if (isStoredInKotlinArchive?.orNull == true) {
                addOutgoingKarArtifactTo(karTaskProvider)
            }
            for (capability in platformComponentCapabilityIds?.orNull.orEmpty()) {
                // TODO: why we even call this code for unpublished modules?
                if (capability.group.isNotBlank()) {
                    outgoing.capability("${capability.group}:${capability.name}:${capability.version}")
                }
            }
        }
    )
}
