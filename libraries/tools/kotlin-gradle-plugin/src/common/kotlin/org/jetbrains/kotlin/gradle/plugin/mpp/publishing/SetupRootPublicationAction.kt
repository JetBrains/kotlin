/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.publishing

import org.gradle.api.Project
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.metadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine
import org.jetbrains.kotlin.gradle.plugin.KotlinPublicationFormat
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultKotlinUsageContext
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinTargetSoftwareComponent
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsageContext
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinVariant
import org.jetbrains.kotlin.gradle.plugin.mpp.archive.KotlinTargetWithKotlinArchiveSupport
import org.jetbrains.kotlin.gradle.plugin.mpp.archive.defaultKotlinUsageContextMaybeReplacedWithKar
import org.jetbrains.kotlin.gradle.plugin.mpp.getHostSpecificMainSharedSourceSets
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.sourcesJarTaskNamed
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.KmpPublicationStrategy
import org.jetbrains.kotlin.gradle.plugin.sources.awaitPlatformCompilations
import org.jetbrains.kotlin.gradle.plugin.sources.defaultImpl
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.targets.metadata.awaitMetadataCompilationsCreated
import org.jetbrains.kotlin.gradle.targets.metadata.getCommonSourceSetsForMetadataCompilation
import org.jetbrains.kotlin.gradle.targets.metadata.getPublishedPlatformCompilations
import org.jetbrains.kotlin.gradle.utils.Future
import org.jetbrains.kotlin.gradle.utils.dashSeparatedName
import org.jetbrains.kotlin.gradle.utils.future
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.io.File

private fun Project.addSourcesJarArtifactToConfiguration(
    configurationName: String,
    classifierPrefix: String?,
    sourcesJarTask: TaskProvider<Jar>,
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

private suspend fun KotlinMultiplatformExtension.sourcesJarContent(): Map<String, Iterable<File>> {
    KotlinPluginLifecycle.Stage.AfterFinaliseDsl.await()
    return buildSet {
        addAll(getCommonSourceSetsForMetadataCompilation(project))
        addAll(getHostSpecificMainSharedSourceSets(project))
        if (publishing.publicationFormat.get() == KotlinPublicationFormat.KOTLIN_ARCHIVE) {
            val platformCompilationsInKotlinArchive = getPublishedPlatformCompilations(project).values
                .filter {
                    val target = it.target
                    target is KotlinTargetWithKotlinArchiveSupport && target.isStoredInKotlinArchive.get()
                }
            for (sourceSet in awaitSourceSets()) {
                if (sourceSet.internal.awaitPlatformCompilations().any { it in platformCompilationsInKotlinArchive }) {
                    add(sourceSet)
                }
            }
        }
    }.associate { it.name to it.defaultImpl.allKotlin }
}


/**
 * This component stores usages related to metadata target.
 *
 * As implementation detail, it reuses kotlinTargetSoftwareComponent, as it
 * already correctly handles creating -published variants, which is
 * required to handle replacement of metadata jar with kotlin archive.
 */
private suspend fun KotlinMultiplatformExtension.metadataVariantsSoftwareComponent(
    sourcesJarTask: TaskProvider<Jar>
) : SoftwareComponent {
    val mainCompilation = metadataTarget.awaitMetadataCompilationsCreated().getByName(MAIN_COMPILATION_NAME)
    val usages = buildSet {
        add(
            project.defaultKotlinUsageContextMaybeReplacedWithKar(
                isStoredInKotlinArchive = publishing.publicationFormat.map { it == KotlinPublicationFormat.KOTLIN_ARCHIVE },
                compilation = mainCompilation,
                mavenScope = KotlinUsageContext.MavenScope.COMPILE,
                dependencyConfigurationName = metadataTarget.apiElementsConfigurationName,
            )
        )

        val sourcesElements = metadataTarget.sourcesElementsConfigurationName
        if (metadataTarget.isSourcesPublishable) {
            project.addSourcesJarArtifactToConfiguration(
                sourcesElements,
                classifierPrefix = when (project.kotlinPropertiesProvider.kmpPublicationStrategy) {
                    KmpPublicationStrategy.UklibPublicationInASingleComponentWithKMPPublication -> "metadata"
                    KmpPublicationStrategy.StandardKMPPublication -> null
                },
                sourcesJarTask
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
    val variant = KotlinVariant(mainCompilation, usages)
    /*
     * Wrapping with KotlinTargetSoftwareComponent, creates -published configurations,
     * and handles replacing with KAR.
     *
     * We could potentially always wrap to KotlinTargetSoftwareComponent,
     * but it would lead to renaming of metadata variants in old publication,
     * which is not a breaking change formally, but not nice.
     */
    return if (publishing.publicationFormat.get() == KotlinPublicationFormat.KOTLIN_ARCHIVE) {
        KotlinTargetSoftwareComponent(metadataTarget, variant)
    } else {
        variant
    }
}


private fun KotlinTarget.publishableSoftwareComponents(): List<SoftwareComponent> {
    val targetPublishableComponentNames = internal.kotlinComponents
        .filter { component -> component.publishable }
        .map { component -> component.name }
        .toSet()

    return components.filter { it.name in targetPublishableComponentNames }
}

internal val SetupRootPublicationAction = KotlinProjectSetupCoroutine {
    val multiplatformExtension = project.multiplatformExtensionOrNull ?: return@KotlinProjectSetupCoroutine
    val sourcesJarTask: TaskProvider<Jar> = sourcesJarTaskNamed(
        taskName = "sourcesJar",
        componentName = multiplatformExtension.rootSoftwareComponent.name,
        project = project,
        sourceSets = project.future { multiplatformExtension.sourcesJarContent() },
        artifactNameAppendix = multiplatformExtension.rootSoftwareComponent.name.toLowerCaseAsciiOnly()
    )
    KotlinPluginLifecycle.Stage.AfterFinaliseCompilations.await()

    val metadataVariants = multiplatformExtension.metadataVariantsSoftwareComponent(sourcesJarTask)
    val (targetsStoredInKotlinArchive, targetsNotStoredInKotlinArchive) = multiplatformExtension.awaitTargets()
        .partition { it is KotlinTargetWithKotlinArchiveSupport && it.isStoredInKotlinArchive.get() }

    multiplatformExtension.rootSoftwareComponent.referencedSoftwareComponents.complete(
        targetsNotStoredInKotlinArchive.flatMap { it.publishableSoftwareComponents() }
    )
    multiplatformExtension.rootSoftwareComponent.embeddedSoftwareComponents.complete(
        buildList {
            addAll(targetsStoredInKotlinArchive.flatMap { it.publishableSoftwareComponents() })
            add(metadataVariants)
            add(multiplatformExtension.publishing.adhocSoftwareComponent)
        }
    )
}
