/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.*
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublicationContainer
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.MavenPublicationInternal
import org.gradle.api.publish.maven.tasks.GenerateMavenPom
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.internal.attributes.PUBLISH_COORDINATES_TYPE_ATTRIBUTE
import org.jetbrains.kotlin.gradle.internal.attributes.WITH_PUBLISH_COORDINATES
import org.jetbrains.kotlin.gradle.internal.publishing.ExportKotlinPublishCoordinatesTask
import org.jetbrains.kotlin.gradle.internal.publishing.PublicationCoordinatesProperty
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope
import org.jetbrains.kotlin.gradle.plugin.sources.sourceSetDependencyConfigurationByScope
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.metadata.isKotlinGranularMetadataEnabled
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.tooling.buildKotlinToolingMetadataTask
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly

private val Project.kotlinMultiplatformRootPublicationImpl: CompletableFuture<MavenPublication?>
        by projectStoredProperty { CompletableFuture() }
internal val Project.kotlinMultiplatformRootPublication: Future<MavenPublication?>
    get() = kotlinMultiplatformRootPublicationImpl

internal val MultiplatformPublishingSetupAction = KotlinProjectSetupCoroutine {
    if (isPluginApplied("maven-publish")) {
        if (project.kotlinPropertiesProvider.createDefaultMultiplatformPublications) {
            project.extensions.configure(PublishingExtension::class.java) { publishing ->
                createRootPublication(project, publishing).also(kotlinMultiplatformRootPublicationImpl::complete)
                createTargetPublications(project, publishing)
            }
        } else {
            kotlinMultiplatformRootPublicationImpl.complete(null)
        }
        project.components.add(project.multiplatformExtension.rootSoftwareComponent)
    } else {
        kotlinMultiplatformRootPublicationImpl.complete(null)
    }
}

/**
 * The root publication that references the platform specific publications as its variants
 */
private fun createRootPublication(project: Project, publishing: PublishingExtension): MavenPublication {
    val kotlinSoftwareComponent = project.multiplatformExtension.rootSoftwareComponent

    return publishing.publications.create("kotlinMultiplatform", MavenPublication::class.java).apply {
        from(kotlinSoftwareComponent)
        (this as MavenPublicationInternal).publishWithOriginalFileName()

        addKotlinToolingMetadataArtifactIfNeeded(project)
    }
}

private fun MavenPublication.addKotlinToolingMetadataArtifactIfNeeded(project: Project) {
    val buildKotlinToolingMetadataTask = project.buildKotlinToolingMetadataTask ?: return

    artifact(buildKotlinToolingMetadataTask.map { it.outputFile }) { artifact ->
        artifact.classifier = "kotlin-tooling-metadata"
        artifact.builtBy(buildKotlinToolingMetadataTask)
    }
}

private fun createTargetPublications(project: Project, publishing: PublishingExtension) {
    val kotlin = project.multiplatformExtension
    // Enforce the order of creating the publications, since the metadata publication is used in the other publications:
    kotlin.targets
        .withType(InternalKotlinTarget::class.java)
        .matching { it.publishable }
        .all { kotlinTarget ->
            if (kotlinTarget is KotlinAndroidTarget)
            // Android targets have their variants created in afterEvaluate; TODO handle this better?
                project.whenEvaluated { kotlinTarget.createMavenPublications(publishing.publications) }
            else
                kotlinTarget.createMavenPublications(publishing.publications)
        }
}

private fun InternalKotlinTarget.createMavenPublications(publications: PublicationContainer) {
    val rootPublication = publications.getByName("kotlinMultiplatform") as MavenPublication
    kotlinComponents
        .filter { kotlinComponent -> kotlinComponent.publishableOnCurrentHost }
        .forEach { kotlinComponent ->
            val componentPublication = publications.create(kotlinComponent.name, MavenPublication::class.java).apply {
                val publication = this

                (publication as MavenPublicationInternal).publishWithOriginalFileName()
                artifactId = kotlinComponent.defaultArtifactId

                // do await for usages since older Gradle versions seem to check the files in the variant eagerly:
                // We are deferring this to 'AfterFinaliseCompilations' as safety measure for now.
                project.launchInStage(KotlinPluginLifecycle.Stage.AfterFinaliseCompilations) {
                    val gradleComponent = components.find { kotlinComponent.name == it.name } ?: return@launchInStage
                    from(gradleComponent)

                    @Suppress("DEPRECATION") val pomRewriter = if (project.kotlinPropertiesProvider.kotlinKmpProjectIsolationEnabled) {
                        val lazyResolvedConfigurations = createLazyResolvedConfigurationsFromKotlinComponent(project, kotlinComponent)
                        val artifacts = lazyResolvedConfigurations.map { lazyResolvedConfiguration ->
                            lazyResolvedConfiguration.files
                        }

                        project.tasks.withType(GenerateMavenPom::class.java).configureEach {
                            if (it.name.contains(publication.name.capitalizeAsciiOnly())) {
                                it.dependsOn(artifacts)
                            }
                        }

                        kotlinComponent.addGavVariantToConfigurations(project, publication, rootPublication)

                        PomDependenciesRewriterImpl(lazyResolvedConfigurations)
                    } else {
                        DeprecatedPomDependenciesRewriter(project, kotlinComponent)
                    }

                    val shouldRewritePomDependencies =
                        project.provider { PropertiesProvider(project).keepMppDependenciesIntactInPoms != true }

                    rewritePom(
                        pom,
                        pomRewriter,
                        shouldRewritePomDependencies,
                        dependenciesForPomRewriting(this@createMavenPublications)
                    )
                }
            }

            (kotlinComponent as? KotlinTargetComponentWithPublication)?.publicationDelegate = componentPublication
            onPublicationCreated(componentPublication)
        }
}

private fun KotlinTargetComponent.addGavVariantToConfigurations(
    project: Project,
    publication: MavenPublication,
    rootPublication: MavenPublication,
) {


    internal.usages
        .filter { it.mavenScope != null }
        .map { originalVariantNameFromPublished(it.dependencyConfigurationName) ?: it.dependencyConfigurationName }
        .forEach { configurationName ->

            val configuration = project.configurations.findByName(configurationName) ?: return@forEach
            val task = createExportPublishCoordinatesTask(project, configurationName)
            configuration.addGavSecondaryVariant(task, project, publication, rootPublication)
        }

}

private fun createExportPublishCoordinatesTask(
    project: Project,
    configurationName: String,
) =
    project.locateOrRegisterTask<ExportKotlinPublishCoordinatesTask>(
        lowerCamelCaseName(configurationName, "ExportPublishCoordinates")
    ) { task ->
        task.outputJsonFile.set(
            project.layout.buildDirectory.file("internal/kmp/${lowerCamelCaseName(configurationName, "PublishCoordinates")}.json")
        )
    }

private fun Configuration.addGavSecondaryVariant(
    task: TaskProvider<ExportKotlinPublishCoordinatesTask>,
    project: Project,
    publication: MavenPublication,
    rootPublication: MavenPublication,
) {

    outgoing.variants.create("gavSecondaryVariant") { variant ->
        variant.attributes.setAttribute(PUBLISH_COORDINATES_TYPE_ATTRIBUTE, WITH_PUBLISH_COORDINATES)
        task.configure {
            it.data.set(
                PublicationCoordinatesProperty(
                    project.provider { rootPublication.groupId },
                    project.provider { rootPublication.artifactId },
                    project.provider { rootPublication.version },
                    project.provider { publication.groupId },
                    project.provider {
                        publication.artifactId
                    },
                    project.provider { publication.version },
                )
            )
        }
        variant.artifact(task.map { it.outputJsonFile }) {
            it.builtBy(task)
            it.type = "kotlin-publication-coordinates"
        }

    }
}

private fun rewritePom(
    pom: MavenPom,
    pomRewriter: PomDependenciesRewriter,
    shouldRewritePomDependencies: Provider<Boolean>,
    includeOnlySpecifiedDependencies: Provider<Set<ModuleCoordinates>>?,
) {
    pom.withXml { xml ->
        if (shouldRewritePomDependencies.get())
            pomRewriter.rewritePomMppDependenciesToActualTargetModules(xml, includeOnlySpecifiedDependencies)
    }
}

/**
 * The metadata targets need their POMs to only include the dependencies from the commonMain API configuration.
 * The actual apiElements configurations of metadata targets now contain dependencies from all source sets, but, as the consumers who
 * can't read Gradle module metadata won't resolve a dependency on an MPP to the granular metadata variant and won't then choose the
 * right dependencies for each source set, we put only the dependencies of the legacy common variant into the POM, i.e. commonMain API.
 */
private fun dependenciesForPomRewriting(target: InternalKotlinTarget): Provider<Set<ModuleCoordinates>>? =
    if (target !is KotlinMetadataTarget || !target.project.isKotlinGranularMetadataEnabled)
        null
    else {
        val commonMain = target.project.kotlinExtension.sourceSets.findByName(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME)
        if (commonMain == null)
            null
        else
            target.project.provider {
                val project = target.project

                // Only the commonMain API dependencies can be published for consumers who can't read Gradle project metadata
                val commonMainApi = project.configurations.sourceSetDependencyConfigurationByScope(
                    commonMain,
                    KotlinDependencyScope.API_SCOPE
                )
                val commonMainDependencies = commonMainApi.allDependencies
                commonMainDependencies.map { ModuleCoordinates(it.group, it.name, it.version) }.toSet()
            }
    }

internal fun Configuration.configureSourcesPublicationAttributes(target: KotlinTarget) {
    val project = target.project

    // In order to be consistent with Java Gradle Plugin, set usage attribute for sources variant
    // to be either JAVA_RUNTIME (for jvm) or KOTLIN_RUNTIME (for other targets)
    // the latter isn't a strong requirement since there is no tooling that consume kotlin sources through gradle variants at the moment
    // so consistency with Java Gradle Plugin seemed most desirable choice.
    attributes.setAttribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.producerRuntimeUsage(target))
    attributes.setAttribute(Category.CATEGORY_ATTRIBUTE, project.attributeValueByName(Category.DOCUMENTATION))
    attributes.setAttribute(DocsType.DOCS_TYPE_ATTRIBUTE, project.attributeValueByName(DocsType.SOURCES))
    // Bundling attribute is about component dependencies, external means that they are provided as separate components
    // source variants doesn't have any dependencies (at least at the moment) so there is not much sense to use this attribute
    // however for Java Gradle Plugin compatibility and in order to prevent weird Variant Resolution errors we include this attribute
    attributes.setAttribute(Bundling.BUNDLING_ATTRIBUTE, project.attributeValueByName(Bundling.EXTERNAL))
    usesPlatformOf(target)
}

internal fun HasAttributes.configureResourcesPublicationAttributes(target: KotlinTarget) {
    val project = target.project

    val usage = if (target is KotlinJsIrTarget) {
        KotlinUsages.KOTLIN_RESOURCES_JS
    } else {
        KotlinUsages.KOTLIN_RESOURCES
    }
    attributes.setAttribute(
        Usage.USAGE_ATTRIBUTE,
        project.usageByName(usage)
    )
    attributes.setAttribute(
        LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
        project.objects.named(usage)
    )

    attributes.setAttribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.LIBRARY))
    attributes.setAttribute(Bundling.BUNDLING_ATTRIBUTE, project.attributeValueByName(Bundling.EXTERNAL))

    setUsesPlatformOf(target)
}