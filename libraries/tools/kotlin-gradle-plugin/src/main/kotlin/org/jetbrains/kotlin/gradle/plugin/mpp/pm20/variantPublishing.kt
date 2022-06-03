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
import org.jetbrains.kotlin.gradle.dsl.pm20Extension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.ComputedCapability
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.copyAttributes
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.copyConfigurationForPublishing
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.disambiguateName
import org.jetbrains.kotlin.gradle.plugin.mpp.publishedConfigurationName
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.dashSeparatedName
import java.util.*
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
        fun get(project: Project): VariantPublishingConfigurator =
            project.objects.newInstance(VariantPublishingConfigurator::class.java, project)
    }

    open fun platformComponentName(variant: KotlinGradleVariant) = when (variant.containingModule.publicationMode) {
        Private -> error("software component is prohibited for non-published $variant")
        is Standalone -> variant.disambiguateName("")
        is Embedded -> variant.name // Use the same software component for same-named variants in all modules
    }

    open fun inferMavenScope(variant: KotlinGradleVariant, configurationName: String): String? =
        when {
            configurationName == variant.apiElementsConfiguration.name -> "compile"
            variant is KotlinGradleVariantWithRuntime && configurationName == variant.runtimeElementsConfiguration.name -> "runtime"
            else -> null
        }

    open fun configurePublishing(
        request: PlatformPublicationToMavenRequest
    ) {
        request.fromModule.ifMadePublic {
            val componentName = request.componentName

            registerPlatformModulePublication(
                request.fromModule,
                componentName,
                request.publicationHolder,
                request.variantPublicationRequests
            )

            val publishFromVariants = request.variantPublicationRequests.mapTo(mutableSetOf()) { it.fromVariant }

            // Collecting sources for multiple variants is not yet supported;
            // TODO make callers provide the source variants?
            // The MPP plugin doesn't publish the source artifacts as variants; keep that behavior for legacy-mapped variants for now
            if (
                publishFromVariants.size == 1 &&
                publishFromVariants.none { it is LegacyMappedVariant }
            ) {
                val singlePublishedVariant = publishFromVariants.single()
                configureSourceElementsPublishing(componentName, singlePublishedVariant, request.publicationHolder)
            }

            registerPlatformVariantsInRootModule(request)
        }
    }

    protected open fun configureSourceElementsPublishing(
        componentName: String,
        variant: KotlinGradleVariant,
        publishedModuleHolder: SingleMavenPublishedModuleHolder
    ) {
        val configurationName = variant.disambiguateName("sourceElements")

        // FIXME create this one not only in ifMadePublic but before that unconditionally?
        val docsVariants = DocumentationVariantConfigurator().createSourcesElementsConfiguration(configurationName, variant)

        val docsVariantForPublishing = copyConfigurationForPublishing(
            project,
            docsVariants.name + "-published",
            docsVariants,
            overrideCapabilities = {
                val capability =
                    ComputedCapability.forPublishedPlatformVariant(variant, publishedModuleHolder)
                if (capability != null) {
                    outgoing.capability(capability)
                }
            }
        )

        project.components.withType(AdhocComponentWithVariants::class.java).named(componentName).configure { component ->
            component.addVariantsFromConfiguration(docsVariantForPublishing) { }
        }
    }

    /**
     * Creates the [AdhocComponentWithVariants] named [componentName] for the given [publishConfigurationsWithMavenScopes].
     * At the point [whenShouldRegisterPublication] creates a Maven publication named [componentName] that publishes the created component.
     * Assigns the created Maven publication to the [publishedModuleHolder].
     */
    protected open fun registerPlatformModulePublication(
        module: KotlinGradleModule,
        componentName: String,
        publishedModuleHolder: SingleMavenPublishedModuleHolder,
        variantRequests: Iterable<VariantPublicationRequest>
    ) {
        module.ifMadePublic {
            val platformComponent =
                project.components.withType(AdhocComponentWithVariants::class.java).findByName(componentName)
                    ?: softwareComponentFactory.adhoc(componentName).also { project.components.add(it) }

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
                        ?.let { override -> { attributes -> copyAttributes(override, attributes) } },
                    overrideDependencies = {
                        addAllLater(project.listProperty {
                            replaceProjectDependenciesWithPublishedMavenDependencies(
                                project,
                                originalConfiguration.allDependencies
                            )
                        })
                    },
                    overrideCapabilities = {
                        ComputedCapability.forPublishedPlatformVariant(request.fromVariant, publishedModuleHolder)
                            ?.let(outgoing::capability)
                    }
                )

                platformComponent.addVariantsFromConfiguration(publishedConfiguration) details@{ variantDetails ->
                    mavenScopeOrNull?.let { variantDetails.mapToMavenScope(it) }
                }
            }

            project.pluginManager.withPlugin("maven-publish") {
                val publication = project.extensions.getByType(PublishingExtension::class.java).run {
                    if (module.publicationMode is Standalone) {
                        publications.create(componentName, MavenPublication::class.java).apply {
                            // TODO: remove internal API usage. This prevents Gradle from reporting errors during publication because of multiple
                            //       Maven publications with different coordinates
                            (this as DefaultMavenPublication).isAlias = true

                            from(platformComponent)
                            artifactId = dashSeparatedName(
                                project.name, publishedModuleHolder.defaultPublishedModuleSuffix
                            ).toLowerCase(Locale.ENGLISH)
                        }
                    } else {
                        // TODO still create the publication for embedded module's variant if one with this name is absent in the main module?
                        publications.findByName(componentName) as? MavenPublication
                    }
                }

                if (publication != null) {
                    publishedModuleHolder.assignMavenPublication(publication)
                }
            }
        }
    }

    protected open fun registerPlatformVariantsInRootModule(
        request: PlatformPublicationToMavenRequest
    ) {
        val platformModuleDependencyProvider = project.provider {
            val variants = request.variantPublicationRequests.mapTo(mutableSetOf()) { it.fromVariant }
            val singleVariant = variants.singleOrNull() ?: error("expected single variant: ${variants.joinToString()}") // TODO NOW: test and remove?
            val coordinates = request.publicationHolder.publishedMavenModuleCoordinates
            (project.dependencies.create("${coordinates.group}:${coordinates.name}:${coordinates.version}") as ModuleDependency).apply {
                capabilities {
                    val capability = ComputedCapability.forPublishedPlatformVariant(singleVariant, request.publicationHolder)
                    if (capability != null) {
                        it.requireCapability(capability)
                    }
                }
            }
        }

        val rootSoftwareComponent = when (request.fromModule.publicationMode) {
            Private -> error("expected to be published")
            Embedded -> project.components
                .withType(AdhocComponentWithVariants::class.java)
                .getByName(rootPublicationComponentName(project.pm20Extension.main))
            is Standalone ->
                project.components
                    .withType(AdhocComponentWithVariants::class.java)
                    .getByName(rootPublicationComponentName(request.fromModule))
        }

        request.variantPublicationRequests.forEach { variantRequest ->
            val configuration = variantRequest.publishConfiguration
            project.configurations.create(publishedConfigurationName(configuration.name)).apply {
                isCanBeConsumed = false
                isCanBeResolved = false

                setGradlePublishedModuleCapability(this, request.fromModule)
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
            ComputedCapability.forProjectDependenciesOnModule(variant.containingModule)
        )
    }
}

internal fun KotlinGradleModule.publicationHolder(): SingleMavenPublishedModuleHolder? =
    when (this) {
        is KotlinGradleModuleInternal -> publicationHolder
        is SingleMavenPublishedModuleHolder -> this
        else -> null
    }
