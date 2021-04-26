/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.attributes.*
import org.gradle.api.capabilities.Capability
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.DefaultMavenPublication
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.ComputedCapability
import org.jetbrains.kotlin.gradle.plugin.mpp.publishedConfigurationName
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.dashSeparatedName
import javax.inject.Inject

fun VariantPublishingConfigurator.configureNativeVariantPublication(variant: KotlinNativeVariantInternal) {
    val publishConfigurations = listOfNotNull(
        variant.apiElementsConfigurationName,
        variant.hostSpecificMetadataElementsConfigurationName // host-specific metadata may be absent
    )
    configurePublishing(variant, variant, publishConfigurations)
}

fun VariantPublishingConfigurator.configureSingleVariantPublication(variant: KotlinGradlePublishedVariantWithRuntime) {
    val publishConfigurations = listOf(variant.apiElementsConfigurationName, variant.runtimeElementsConfigurationName)
    configurePublishing(variant, variant, publishConfigurations)
}

open class VariantPublishingConfigurator @Inject constructor(
    private val project: Project,
    private val softwareComponentFactory: SoftwareComponentFactory
) {
    companion object {
        fun get(project: Project): VariantPublishingConfigurator = project.objects.newInstance(VariantPublishingConfigurator::class.java, project)
    }

    open fun platformComponentName(variant: KotlinGradleVariant) = variant.disambiguateName("")

    open fun inferMavenScopes(variant: KotlinGradleVariant, configurationNames: Iterable<String>): Map<String, String?> =
        configurationNames.associateWith { configurationName ->
            when {
                configurationName == variant.apiElementsConfigurationName -> "compile"
                variant is KotlinGradleVariantWithRuntime && configurationName == variant.runtimeElementsConfigurationName -> "runtime"
                else -> null
            }
        }

    open fun configurePublishing(
        variant: KotlinGradleVariant,
        publishedModuleHolder: SingleMavenPublishedModuleHolder,
        publishConfigurations: Iterable<String>
    ) {
        val componentName = platformComponentName(variant)
        val configurationsMap = inferMavenScopes(variant, publishConfigurations)

        registerPlatformModulePublication(
            componentName,
            publishedModuleHolder,
            configurationsMap,
            variant.containingModule::ifMadePublic
        )

        configureSourceElementsPublishing(variant)

        registerPlatformVariantsInRootModule(
            publishedModuleHolder,
            variant.containingModule,
            publishConfigurations
        )
    }

    protected open fun configureSourceElementsPublishing(variant: KotlinGradleVariant) {
        val configurationName = variant.disambiguateName("sourceElements")
        val componentName = platformComponentName(variant)
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
        publishConfigurationsWithMavenScopes: Map<String, String?>,
        whenShouldRegisterPublication: (() -> Unit) -> Unit
    ) {
        val platformComponent = softwareComponentFactory.adhoc(componentName)
        project.components.add(platformComponent)
        publishConfigurationsWithMavenScopes.forEach { (configurationName, mavenScopeOrNull) ->
            platformComponent.addVariantsFromConfiguration(project.configurations.getByName(configurationName)) { variantDetails ->
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
        configurationNames: Iterable<String>
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

        configurationNames.forEach { configurationName ->
            fun <T : Any> copyAttribute(from: AttributeContainer, to: AttributeContainer, key: Attribute<T>) {
                to.attribute(key, checkNotNull(from.getAttribute(key)))
            }

            val originalConfiguration = project.configurations.getByName(configurationName)
            project.configurations.create(publishedConfigurationName(configurationName)).apply {
                isCanBeConsumed = false
                isCanBeResolved = false

                setModuleCapability(this, kotlinModule)
                dependencies.addLater(platformModuleDependencyProvider)
                val originalAttributes = originalConfiguration.attributes
                originalAttributes.keySet().forEach {
                    copyAttribute(originalAttributes, attributes, it)
                }
                rootSoftwareComponent.addVariantsFromConfiguration(this) { }
            }
        }
    }
}

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