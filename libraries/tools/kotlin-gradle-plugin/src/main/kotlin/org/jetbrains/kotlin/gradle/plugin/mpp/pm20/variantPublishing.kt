/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.attributes.*
import org.gradle.api.capabilities.Capability
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.DefaultMavenPublication
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinFragmentModuleCapabilityConfigurator.setModuleCapability
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.ComputedCapability
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.disambiguateName
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.copyAttributes
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.copyConfigurationForPublishing
import org.jetbrains.kotlin.gradle.plugin.mpp.publishedConfigurationName
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.dashSeparatedName
import javax.inject.Inject

interface PlatformPublicationToMavenRequest {
    val componentName: String
    val fromModule: KotlinGradleModule
    val publicationHolder: SingleMavenPublishedModuleHolder
    val variantPublicationRequests: Iterable<VariantPublicationRequest>
}

data class BasicPlatformPublicationToMavenRequest(
    override val componentName: String,
    override val fromModule: KotlinGradleModule,
    override val publicationHolder: SingleMavenPublishedModuleHolder,
    override val variantPublicationRequests: Iterable<VariantPublicationRequest>
) : PlatformPublicationToMavenRequest {
    init {
        check(variantPublicationRequests.all { it.fromVariant.containingModule === fromModule }) {
            "Variants for publication should all belong to the fromModule ($fromModule)"
        }
    }
}

/** TODO: consider also using this class for exposing a KPM variant's configurations for project-to-project dependencies,
 *        so that a variant may expose an arbitrary set of configurations rather just { API, runtime } or { API } */
interface VariantPublicationRequest {
    val fromVariant: KotlinGradleVariant
    val publishConfiguration: Configuration
}

data class BasicVariantPublicationRequest(
    override val fromVariant: KotlinGradleVariant,
    override val publishConfiguration: Configuration
) : VariantPublicationRequest


fun VariantPublishingConfigurator.configureNativeVariantPublication(variant: KotlinNativeVariantInternal) {
    val publishConfigurations = listOfNotNull(
        variant.apiElementsConfiguration,
        variant.hostSpecificMetadataElementsConfiguration // host-specific metadata may be absent
    )
    configureSingleVariantPublishing(variant, variant, publishConfigurations)
}

fun VariantPublishingConfigurator.configureSingleVariantPublication(variant: KotlinGradlePublishedVariantWithRuntime) {
    val publishConfigurations = listOf(variant.apiElementsConfiguration, variant.runtimeElementsConfiguration)
    configureSingleVariantPublishing(variant, variant, publishConfigurations)
}

fun VariantPublishingConfigurator.configureSingleVariantPublishing(
    variant: KotlinGradleVariant,
    publishedModuleHolder: SingleMavenPublishedModuleHolder,
    publishConfigurations: Iterable<Configuration>
) {
    configurePublishing(
        BasicPlatformPublicationToMavenRequest(
            platformComponentName(variant),
            variant.containingModule,
            publishedModuleHolder,
            publishConfigurations.map {
                BasicVariantPublicationRequest(variant, it)
            }
        )
    )
}

open class VariantPublishingConfigurator @Inject constructor(
    private val project: Project,
    private val softwareComponentFactory: SoftwareComponentFactory
) {
    companion object {
        fun get(project: Project): VariantPublishingConfigurator = project.objects.newInstance(VariantPublishingConfigurator::class.java, project)
    }

    open fun platformComponentName(variant: KotlinGradleVariant) = variant.disambiguateName("")

    open fun inferMavenScope(variant: KotlinGradleVariant, configurationName: String): String? =
        when {
            configurationName == variant.apiElementsConfiguration.name -> "compile"
            variant is KotlinGradleVariantWithRuntime && configurationName == variant.runtimeElementsConfiguration.name -> "runtime"
            else -> null
        }

    open fun configurePublishing(
        request: PlatformPublicationToMavenRequest
    ) {
        val componentName = request.componentName
        val configurationsMap = request.variantPublicationRequests.associate {
            it.publishConfiguration to inferMavenScope(it.fromVariant, it.publishConfiguration.name)
        }

        registerPlatformModulePublication(
            componentName,
            request.publicationHolder,
            request.variantPublicationRequests,
            request.fromModule::ifMadePublic
        )

        val publishFromVariants = request.variantPublicationRequests.mapTo(mutableSetOf()) { it.fromVariant }

        // Collecting sources for multiple variants is not yet supported;
        // TODO make callers provide the source variants?
        if (publishFromVariants.size == 1) {
            val singlePublishedVariant = publishFromVariants.single()
            configureSourceElementsPublishing(componentName, singlePublishedVariant)
        }

        registerPlatformVariantsInRootModule(
            request.publicationHolder,
            request.fromModule,
            request.variantPublicationRequests
        )
    }

    protected open fun configureSourceElementsPublishing(componentName: String, variant: KotlinGradleVariant) {
        val configurationName = variant.disambiguateName("sourceElements")
        val docsVariants = DocumentationVariantConfigurator().createSourcesElementsConfiguration(configurationName, variant)
        project.components.withType(AdhocComponentWithVariants::class.java).named(componentName).configure { component ->
            component.addVariantsFromConfiguration(docsVariants) { }
        }
    }

    /**
     * Creates the [AdhocComponentWithVariants] named [componentName] for the given [publishConfigurationsWithMavenScopes].
     * At the point [whenShouldRegisterPublication] creates a Maven publication named [componentName] that publishes the created component.
     * Assigns the created Maven publication to the [publishedModuleHolder].
     */
    protected open fun registerPlatformModulePublication(
        componentName: String,
        publishedModuleHolder: SingleMavenPublishedModuleHolder,
        variantRequests: Iterable<VariantPublicationRequest>,
        whenShouldRegisterPublication: (() -> Unit) -> Unit
    ) {
        val platformComponent = softwareComponentFactory.adhoc(componentName)
        project.components.add(platformComponent)

        variantRequests.forEach { request ->
            val originalConfiguration = request.publishConfiguration
            val mavenScopeOrNull = inferMavenScope(request.fromVariant, originalConfiguration.name)

            val publishedConfiguration = copyConfigurationForPublishing(
                request.fromVariant.project,
                newName = publishedConfigurationName(originalConfiguration.name) + "-platform",
                configuration = originalConfiguration,
                overrideArtifacts = (request as? AdvancedVariantPublicationRequest)
                    ?.overrideConfigurationArtifactsForPublication
                    ?.let { override -> { artifacts -> artifacts.addAllLater(override) } },
                overrideAttributes = (request as? AdvancedVariantPublicationRequest)
                    ?.overrideConfigurationAttributesForPublication
                    ?.let { override -> { attributes -> copyAttributes(override, attributes) } }
            )

            platformComponent.addVariantsFromConfiguration(publishedConfiguration) { variantDetails ->
                mavenScopeOrNull?.let { variantDetails.mapToMavenScope(it) }
            }
        }

        whenShouldRegisterPublication {
            project.pluginManager.withPlugin("maven-publish") {
                project.extensions.getByType(PublishingExtension::class.java).apply {
                    publications.create(componentName, MavenPublication::class.java).apply {
                        (this as DefaultMavenPublication).isAlias = true
                        from(platformComponent)
                        publishedModuleHolder.assignMavenPublication(this)
                        artifactId = dashSeparatedName(project.name, publishedModuleHolder.defaultPublishedModuleSuffix)
                    }
                }
            }
        }
    }

    protected open fun registerPlatformVariantsInRootModule(
        publishedModuleHolder: SingleMavenPublishedModuleHolder,
        kotlinModule: KotlinGradleModule,
        variantRequests: Iterable<VariantPublicationRequest>
    ) {
        val platformModuleDependencyProvider = project.provider {
            val coordinates = publishedModuleHolder.publishedMavenModuleCoordinates
            (project.dependencies.create("${coordinates.group}:${coordinates.name}:${coordinates.version}") as ModuleDependency).apply {
                if (kotlinModule.moduleClassifier != null) {
                    capabilities { it.requireCapability(ComputedCapability.fromModule(kotlinModule)) }
                }
            }
        }

        val rootSoftwareComponent =
            project.components
                .withType(AdhocComponentWithVariants::class.java)
                .getByName(rootPublicationComponentName(kotlinModule))

        variantRequests.forEach { variantRequest ->
            val configuration = variantRequest.publishConfiguration
            project.configurations.create(publishedConfigurationName(configuration.name)).apply {
                isCanBeConsumed = false
                isCanBeResolved = false

                setModuleCapability(this, kotlinModule)
                dependencies.addLater(platformModuleDependencyProvider)
                copyAttributes(configuration.attributes, this.attributes)
                rootSoftwareComponent.addVariantsFromConfiguration(this) { }
            }
        }
    }
}

internal data class AdvancedVariantPublicationRequest(
    override val fromVariant: KotlinGradleVariant,
    override val publishConfiguration: Configuration,
    val overrideConfigurationAttributesForPublication: AttributeContainer?,
    val overrideConfigurationArtifactsForPublication: Provider<out Iterable<PublishArtifact>>?,
    val includeIntoProjectStructureMetadata: Boolean
) : VariantPublicationRequest

open class DocumentationVariantConfigurator {
    open fun createSourcesElementsConfiguration(
        project: Project,
        configurationName: String,
        sourcesArtifactProvider: AbstractArchiveTask,
        artifactClassifier: String,
        capability: Capability?
    ): Configuration {
        return project.configurations.create(configurationName).apply {
            isCanBeResolved = false
            isCanBeConsumed = true
            attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_SOURCES))
            attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category::class.java, Category.DOCUMENTATION))
            attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, project.objects.named(Bundling::class.java, Bundling.EXTERNAL))
            attributes.attribute(DocsType.DOCS_TYPE_ATTRIBUTE, project.objects.named(DocsType::class.java, DocsType.SOURCES))
            outgoing.artifact(sourcesArtifactProvider) {
                it.classifier = artifactClassifier
            }
            if (capability != null) {
                outgoing.capability(capability)
            }
        }
    }

    open fun createSourcesElementsConfiguration(
        configurationName: String,
        variant: KotlinGradleVariant
    ): Configuration {
        val sourcesArtifactTask = variant.project.tasks.withType<AbstractArchiveTask>().named(variant.sourceArchiveTaskName)
        val artifactClassifier = dashSeparatedName(variant.containingModule.moduleClassifier, "sources")
        return createSourcesElementsConfiguration(
            variant.project,
            configurationName,
            sourcesArtifactTask.get(),
            artifactClassifier,
            ComputedCapability.fromModuleOrNull(variant.containingModule)
        )
    }
}