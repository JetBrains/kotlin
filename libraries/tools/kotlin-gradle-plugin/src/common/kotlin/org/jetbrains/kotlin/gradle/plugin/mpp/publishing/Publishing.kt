/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.publishing

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.*
import org.gradle.api.publish.PublicationContainer
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.MavenPublicationInternal
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.InternalKotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinTargetComponentWithPublication
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.tooling.buildKotlinToolingMetadataTask
import org.jetbrains.kotlin.gradle.utils.*

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
            /** Publication for [KotlinMetadataTarget] is created in [createRootPublication] */
            if (kotlinTarget is KotlinMetadataTarget) return@all
            if (kotlinTarget is KotlinAndroidTarget)
            // Android targets have their variants created in afterEvaluate; TODO handle this better?
                project.whenEvaluated { kotlinTarget.createTargetSpecificMavenPublications(publishing.publications) }
            else
                kotlinTarget.createTargetSpecificMavenPublications(publishing.publications)
        }
}

private fun InternalKotlinTarget.createTargetSpecificMavenPublications(publications: PublicationContainer) {
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
                    publication.from(gradleComponent)

                    project.rewriteKmpDependenciesInPomForTargetPublication(kotlinComponent, publication)
                }
            }

            (kotlinComponent as? KotlinTargetComponentWithPublication)?.publicationDelegate = componentPublication
            onPublicationCreated(componentPublication)
        }
}

internal fun Configuration.configureSourcesPublicationAttributes(target: KotlinTarget) {
    val project = target.project

    // In order to be consistent with Java Gradle Plugin, set usage attribute for sources variant
    // to be either JAVA_RUNTIME (for jvm) or KOTLIN_RUNTIME (for other targets)
    // the latter isn't a strong requirement since there is no tooling that consume kotlin sources through gradle variants at the moment
    // so consistency with Java Gradle Plugin seemed most desirable choice.
    KotlinUsages.configureProducerRuntimeUsage(this, target)
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