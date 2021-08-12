/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.attributes.*
import org.gradle.api.capabilities.Capability
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.DefaultMavenPublication
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.dsl.pm20Extension
import org.jetbrains.kotlin.gradle.dsl.topLevelExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.ComputedCapability
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.copyAttributes
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.copyConfigurationForPublishing
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.listProperty
import org.jetbrains.kotlin.gradle.plugin.mpp.publishedConfigurationName
import org.jetbrains.kotlin.gradle.plugin.mpp.toModuleDependency
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.dashSeparatedName
import org.jetbrains.kotlin.project.model.KotlinModuleDependency
import org.jetbrains.kotlin.project.model.LocalModuleIdentifier
import org.jetbrains.kotlin.project.model.MavenModuleIdentifier
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
        fun get(project: Project): VariantPublishingConfigurator =
            project.objects.newInstance(VariantPublishingConfigurator::class.java, project)
    }

    open fun platformComponentName(variant: KotlinGradleVariant) = when (variant.containingModule.publicationMode) {
        Private -> error("software component is prohibited for non-published $variant")
        is Standalone -> variant.disambiguateName("")
        is Embedded -> variant.name // Use the same software component for same-named variants in all modules
    }

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

        variant.containingModule.ifMadePublic {
            val componentName = platformComponentName(variant)
            val configurationsMap = inferMavenScopes(variant, publishConfigurations)

            registerPlatformModulePublication(
                variant,
                componentName,
                publishedModuleHolder,
                configurationsMap
            )
            configureSourceElementsPublishing(variant, publishedModuleHolder)
            registerPlatformVariantsInRootModule(
                variant,
                publishedModuleHolder,
                variant.containingModule,
                publishConfigurations
            )
        }
    }

    protected open fun configureSourceElementsPublishing(
        variant: KotlinGradleVariant,
        publishedModuleHolder: SingleMavenPublishedModuleHolder
    ) {
        val configurationName = variant.disambiguateName("sourceElements")
        val componentName = platformComponentName(variant)

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
        variant: KotlinGradleVariant,
        componentName: String,
        publishedModuleHolder: SingleMavenPublishedModuleHolder,
        publishConfigurationsWithMavenScopes: Map<String, String?>
    ) {
        val module = variant.containingModule
        module.ifMadePublic {
            val platformComponent =
                project.components.withType(AdhocComponentWithVariants::class.java).findByName(componentName)
                    ?: softwareComponentFactory.adhoc(componentName).also { project.components.add(it) }

            publishConfigurationsWithMavenScopes.forEach { (configurationName, mavenScopeOrNull) ->
                val originalConfiguration = project.configurations.getByName(configurationName)

                val publishedConfiguration = copyConfigurationForPublishing(
                    project,
                    publishedConfigurationName(configurationName) + "-platform",
                    originalConfiguration,
                    overrideDependencies = {
                        addAllLater(project.listProperty {
                            replaceProjectDependenciesWithPublishedMavenDependencies(
                                project,
                                originalConfiguration.allDependencies
                            )
                        })
                    },
                    overrideCapabilities = {
                        ComputedCapability.forPublishedPlatformVariant(variant, publishedModuleHolder)
                            ?.let(outgoing::capability)
                    }
                )

                platformComponent.addVariantsFromConfiguration(publishedConfiguration) { variantDetails ->
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
                            artifactId = dashSeparatedName(project.name, publishedModuleHolder.defaultPublishedModuleSuffix)
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
        variant: KotlinGradleVariant,
        publishedModuleHolder: SingleMavenPublishedModuleHolder,
        kotlinModule: KotlinGradleModule,
        configurationNames: Iterable<String>
    ) {
        val platformModuleDependencyProvider = project.provider {
            val coordinates = publishedModuleHolder.publishedMavenModuleCoordinates
            (project.dependencies.create("${coordinates.group}:${coordinates.name}:${coordinates.version}") as ModuleDependency).apply {
                capabilities {
                    val capability = ComputedCapability.forPublishedPlatformVariant(variant, publishedModuleHolder)
                    if (capability != null) {
                        it.requireCapability(capability)
                    }
                }
            }
        }

        val rootSoftwareComponent = when (kotlinModule.publicationMode) {
            Private -> error("expected to be published")
            Embedded -> project.components
                .withType(AdhocComponentWithVariants::class.java)
                .getByName(rootPublicationComponentName(project.pm20Extension.main))
            is Standalone ->
                project.components
                    .withType(AdhocComponentWithVariants::class.java)
                    .getByName(rootPublicationComponentName(kotlinModule))
        }

        configurationNames.forEach { configurationName ->
            val originalConfiguration = project.configurations.getByName(configurationName)
            project.configurations.create(publishedConfigurationName(configurationName)).apply {
                isCanBeConsumed = false
                isCanBeResolved = false

                setGradlePublishedModuleCapability(this, kotlinModule)
                dependencies.addLater(platformModuleDependencyProvider)
                copyAttributes(originalConfiguration.attributes, attributes)
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
            ComputedCapability.forProjectDependenciesOnModule(variant.containingModule)
        )
    }
}

internal data class VersionedMavenModuleIdentifier(val moduleId: MavenModuleIdentifier, val version: String)

internal fun localModuleDependenciesToPublishedModuleMapping(
    project: Project,
    dependencies: Iterable<KotlinModuleDependency>
): Map<KotlinModuleDependency, VersionedMavenModuleIdentifier> {
    return dependencies.mapNotNull mapping@{ dependency ->
        val moduleIdentifier = dependency.moduleIdentifier
        val resolvesToProject = dependency.moduleIdentifier.let {
            if (it is LocalModuleIdentifier && it.buildId == project.currentBuildId().name)
                project.project(it.projectId)
            else null
        }
        if (resolvesToProject == null)
            null
        else {
            val moduleClassifier = moduleIdentifier.moduleClassifier
            when (val ext = resolvesToProject.topLevelExtensionOrNull) {
                is KotlinPm20ProjectExtension -> {
                    val module = ext.modules.find { it.moduleClassifier == moduleClassifier }
                    if (module == null)
                        null
                    else {
                        when (module.publicationMode) {
                            Private -> error("A dependency on ${module} can't be published because the module is not published.")
                            is Standalone, Embedded -> module.publicationHolder()
                                ?.publishedMavenModuleCoordinates
                                ?.let {
                                    dependency to VersionedMavenModuleIdentifier(
                                        MavenModuleIdentifier(
                                            it.group,
                                            it.name,
                                            module.moduleClassifier.takeIf { module.publicationMode is Embedded }
                                        ),
                                        it.version
                                    )
                                }
                        }
                    }
                }
                is KotlinMultiplatformExtension -> {
                    val rootPublication = ext.rootSoftwareComponent.publicationDelegate
                    val group = rootPublication?.groupId ?: project.group.toString()
                    val name = rootPublication?.artifactId ?: project.name
                    val version = rootPublication?.version ?: project.version.toString()
                    dependency to VersionedMavenModuleIdentifier(MavenModuleIdentifier(group, name, null), version)
                }
                else -> null
            }
        }
    }.toMap()
}

internal fun replaceProjectDependenciesWithPublishedMavenIdentifiers(
    project: Project,
    dependencies: Iterable<KotlinModuleDependency>
): Set<MavenModuleIdentifier> {
    val mapping = localModuleDependenciesToPublishedModuleMapping(project, dependencies)
    return dependencies.mapNotNull { dependency ->
        val replacement = mapping[dependency]
        val id = dependency.moduleIdentifier
        if (replacement != null)
            replacement.moduleId
        else if (id is MavenModuleIdentifier)
            id
        else if (id is LocalModuleIdentifier && id.buildId == project.currentBuildId().name) {
            val otherProject = project.project(id.projectId)
            // TODO: find single publication with maven-publish in non-MPP projects?
            MavenModuleIdentifier(otherProject.group.toString(), otherProject.name, otherProject.version.toString())
        } else null
    }.toSet()
}

internal fun replaceProjectDependenciesWithPublishedMavenDependencies(
    project: Project,
    dependencies: Iterable<Dependency>
): List<Dependency> {
    val dependencyToKotlinModuleDependency = dependencies.associateWith { it.toModuleDependency(project) }
    val mapping = localModuleDependenciesToPublishedModuleMapping(project, dependencyToKotlinModuleDependency.values)
    return dependencies.map { dependency ->
        val replacement = mapping[dependencyToKotlinModuleDependency.getValue(dependency)]
        if (replacement != null)
            project.dependencies.create("${replacement.moduleId.group}:${replacement.moduleId.name}:${replacement.version}").apply {
                if (replacement.moduleId.moduleClassifier != null) {
                    (this as ModuleDependency).capabilities {
                        it.requireCapability(checkNotNull(ComputedCapability.forAuxiliaryModuleByCoordinatesAndName(project, replacement)))
                    }
                }
            }
        else dependency
    }

}

internal fun KotlinGradleModule.publicationHolder(): SingleMavenPublishedModuleHolder? =
    when (this) {
        is KotlinGradleModuleInternal -> publicationHolder
        is SingleMavenPublishedModuleHolder -> this
        else -> null
    }
