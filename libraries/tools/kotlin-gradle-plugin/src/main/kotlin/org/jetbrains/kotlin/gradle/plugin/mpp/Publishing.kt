/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublicationContainer
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.MavenPublicationInternal
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope
import org.jetbrains.kotlin.gradle.plugin.sources.sourceSetDependencyConfigurationByScope
import org.jetbrains.kotlin.gradle.plugin.whenEvaluated
import org.jetbrains.kotlin.gradle.targets.metadata.isKotlinGranularMetadataEnabled
import org.jetbrains.kotlin.gradle.tooling.BuildKotlinToolingMetadataTask

internal fun configurePublishingWithMavenPublish(project: Project) = project.pluginManager.withPlugin("maven-publish") {
    project.extensions.configure(PublishingExtension::class.java) { publishing ->
        createRootPublication(project, publishing)
        createTargetPublications(project, publishing)
    }

    project.components.add(project.multiplatformExtension.rootSoftwareComponent)
}

/**
 * The root publication that references the platform specific publications as its variants
 */
private fun createRootPublication(project: Project, publishing: PublishingExtension) {
    val kotlinSoftwareComponent = project.multiplatformExtension.rootSoftwareComponent

    publishing.publications.create("kotlinMultiplatform", MavenPublication::class.java).apply {
        from(kotlinSoftwareComponent)
        (this as MavenPublicationInternal).publishWithOriginalFileName()
        kotlinSoftwareComponent.publicationDelegate = this@apply
        kotlinSoftwareComponent.sourcesArtifacts.forEach { sourceArtifact ->
            artifact(sourceArtifact)
        }

        addKotlinToolingMetadataArtifactIfNeeded(project)
    }
}

private fun MavenPublication.addKotlinToolingMetadataArtifactIfNeeded(project: Project) {
    if (!PropertiesProvider(project).enableKotlinToolingMetadataArtifact) return
    val buildKotlinToolingMetadataTask = project.tasks.withType(BuildKotlinToolingMetadataTask::class.java)
        .named(BuildKotlinToolingMetadataTask.defaultTaskName) ?: return


    artifact(buildKotlinToolingMetadataTask.flatMap { it.outputFile }) { artifact ->
        artifact.classifier = "kotlin-tooling-metadata"
        artifact.builtBy(buildKotlinToolingMetadataTask)
    }
}

private fun createTargetPublications(project: Project, publishing: PublishingExtension) {
    val kotlin = project.multiplatformExtension
    // Enforce the order of creating the publications, since the metadata publication is used in the other publications:
    kotlin.targets
        .withType(AbstractKotlinTarget::class.java)
        .matching { it.publishable }
        .all { kotlinTarget ->
            if (kotlinTarget is KotlinAndroidTarget)
            // Android targets have their variants created in afterEvaluate; TODO handle this better?
                project.whenEvaluated { kotlinTarget.createMavenPublications(publishing.publications) }
            else
                kotlinTarget.createMavenPublications(publishing.publications)
        }
}

private fun AbstractKotlinTarget.createMavenPublications(publications: PublicationContainer) {
    components
        .map { gradleComponent -> gradleComponent to kotlinComponents.single { it.name == gradleComponent.name } }
        .filter { (_, kotlinComponent) -> kotlinComponent.publishable }
        .forEach { (gradleComponent, kotlinComponent) ->
            val componentPublication = publications.create(kotlinComponent.name, MavenPublication::class.java).apply {
                // do this in whenEvaluated since older Gradle versions seem to check the files in the variant eagerly:
                project.whenEvaluated {
                    from(gradleComponent)
                    kotlinComponent.sourcesArtifacts.forEach { sourceArtifact ->
                        artifact(sourceArtifact)
                    }
                }
                (this as MavenPublicationInternal).publishWithOriginalFileName()
                artifactId = kotlinComponent.defaultArtifactId

                val pomRewriter = PomDependenciesRewriter(project, kotlinComponent)
                val shouldRewritePomDependencies =
                    project.provider { PropertiesProvider(project).keepMppDependenciesIntactInPoms != true }

                rewritePom(
                    pom,
                    pomRewriter,
                    shouldRewritePomDependencies,
                    dependenciesForPomRewriting(this@createMavenPublications)
                )
            }

            (kotlinComponent as? KotlinTargetComponentWithPublication)?.publicationDelegate = componentPublication
            publicationConfigureActions.all { it.execute(componentPublication) }
        }
}

private fun rewritePom(
    pom: MavenPom,
    pomRewriter: PomDependenciesRewriter,
    shouldRewritePomDependencies: Provider<Boolean>,
    includeOnlySpecifiedDependencies: Provider<Set<ModuleCoordinates>>?
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
private fun dependenciesForPomRewriting(target: AbstractKotlinTarget): Provider<Set<ModuleCoordinates>>? =
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
                val commonMainApi = project.sourceSetDependencyConfigurationByScope(commonMain, KotlinDependencyScope.API_SCOPE)
                val commonMainDependencies = commonMainApi.allDependencies
                commonMainDependencies.map { ModuleCoordinates(it.group, it.name, it.version) }.toSet()
            }
    }
