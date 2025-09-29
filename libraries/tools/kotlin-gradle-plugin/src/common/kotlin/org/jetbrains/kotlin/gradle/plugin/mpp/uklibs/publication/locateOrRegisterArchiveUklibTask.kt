/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.awaitMetadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.*
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.targets.metadata.awaitMetadataCompilationsCreated
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.utils.createConsumable
import org.jetbrains.kotlin.gradle.utils.maybeCreateConsumable

internal const val UKLIB_API_ELEMENTS_NAME = "uklibApiElements"
internal const val UKLIB_RUNTIME_ELEMENTS_NAME = "uklibRuntimeElements"

internal const val UKLIB_JAVA_API_ELEMENTS_STUB_NAME = "javaApiElements"
internal const val UKLIB_JAVA_RUNTIME_ELEMENTS_STUB_NAME = "javaRuntimeElements"

internal suspend fun Project.createUklibOutgoingVariantsAndPublication(): List<DefaultKotlinUsageContext> {
    val taskName = "archiveUklib"
    val archiveUklib = tasks.register(taskName, ArchiveUklibTask::class.java)

    val kgpFragments = multiplatformExtension.validateKgpModelIsUklibCompliantAndCreateKgpFragments()

    archiveUklib.configure { task ->
        kgpFragments.forEach {
            task.fragments.add(
                it.fragment
            )
        }
    }

    val uklibUsages = createOutgoingUklibConfigurationsAndUsages(
        archiveUklib,
        kgpFragments
    )

    return uklibUsages
}

internal fun Project.locateOrRegisterUklibManifestSerializationWithoutCompilationDependency(): TaskProvider<SerializeMetadataFragmentsOnlyUklibManifest>? {
    return when (project.kotlinPropertiesProvider.kmpPublicationStrategy) {
        KmpPublicationStrategy.UklibPublicationInASingleComponentWithKMPPublication -> project.locateOrRegisterTask<SerializeMetadataFragmentsOnlyUklibManifest>(
            "serializeUklibManifestWithoutCompilationDependency"
        )
        KmpPublicationStrategy.StandardKMPPublication -> null
    }
}

internal fun Project.maybeCreateUklibApiElements() = configurations.maybeCreateConsumable(UKLIB_API_ELEMENTS_NAME)
internal fun Project.maybeCreateUklibRuntimeElements() = configurations.maybeCreateConsumable(UKLIB_RUNTIME_ELEMENTS_NAME)

private suspend fun Project.createOutgoingUklibConfigurationsAndUsages(
    archiveTask: TaskProvider<ArchiveUklibTask>,
    publishedCompilations: List<KGPUklibFragment>,
): List<DefaultKotlinUsageContext> {
    /**
     * FIXME: We can still enter the transforms for interproject dependencies with missing files.
     */
    val uklibApiElements = maybeCreateUklibApiElements().apply {
        attributes.apply {
            attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_UKLIB_API))
            attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
            attribute(isUklib, isUklibTrue)
        }
        inheritCompilationDependenciesFromPublishedCompilations(publishedCompilations.map { it.compilation })
        isVisible = false
    }

    val metadataCompilations = publishedCompilations.filter { it.compilation.platformType == KotlinPlatformType.common }
    val serializeUklibManifestWithoutMetadataCompilationDependencies = locateOrRegisterUklibManifestSerializationWithoutCompilationDependency()
            ?: error("serializeUklibManifestWithoutCompilationDependency task must be available")

    serializeUklibManifestWithoutMetadataCompilationDependencies.configure { task ->
        metadataCompilations.forEach {
            task.metadataFragments.add(it.fragment)
        }
    }
    uklibApiElements.outgoing.variants.create("uklibInterprojectMetadata") {
        it.attributes.attribute(uklibStateAttribute, uklibStateDecompressed)
        it.attributes.attribute(uklibViewAttribute, uklibViewAttributeWholeUklib)
        it.artifact(serializeUklibManifestWithoutMetadataCompilationDependencies) {
            it.extension = uklibManifestArtifactType
        }
    }
    /**
     * This outgoing configuration is used to inject interproject metadata compilation output dependencies in uklibs. See
     * [org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyTransformationTaskInputs.interprojectUklibMetadataCompilationOutputsView]
     */
    uklibApiElements.outgoing.variants.create("uklibInterprojectMetadataCompilationOutputs") {
        it.attributes.attribute(uklibStateAttribute, uklibStateDecompressed)
        it.attributes.attribute(uklibViewAttribute, uklibViewAttributeMetadataCompilationOutputs)
        // This artifact is never actually created or read
        it.artifact(layout.buildDirectory.file("kotlin/uklibMetadataCompilationOutputs")) {
            it.builtBy(metadataCompilations.map { it.fragment })
        }
    }

    val uklibRuntimeElements = maybeCreateUklibRuntimeElements().apply {
        attributes.apply {
            attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_UKLIB_RUNTIME))
            attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
            attribute(isUklib, isUklibTrue)
        }
        inheritRuntimeDependenciesFromPublishedCompilations(publishedCompilations.map { it.compilation })
        isVisible = false
    }

    /**
     * These will be used as the fallback when the secondary platform variant is not available in interproject dependency
     */
    uklibApiElements.outgoing.variants.create("fallback") {
        it.attributes.attribute(uklibStateAttribute, uklibStateDecompressed)
    }
    uklibRuntimeElements.outgoing.variants.create("fallback") {
        it.attributes.attribute(uklibStateAttribute, uklibStateDecompressed)
    }

    project.artifacts.add(UKLIB_API_ELEMENTS_NAME, archiveTask) {
        it.extension = Uklib.UKLIB_EXTENSION
    }
    project.artifacts.add(UKLIB_RUNTIME_ELEMENTS_NAME, archiveTask) {
        it.extension = Uklib.UKLIB_EXTENSION
    }

    val metadataTarget = multiplatformExtension.awaitMetadataTarget()
    val anyMetadataCompilation = metadataTarget.awaitMetadataCompilationsCreated().first()
    val variants = mutableListOf(
        DefaultKotlinUsageContext(
            compilation = anyMetadataCompilation,
            dependencyConfigurationName = UKLIB_RUNTIME_ELEMENTS_NAME,
            includeIntoProjectStructureMetadata = false,
        ),
        DefaultKotlinUsageContext(
            compilation = anyMetadataCompilation,
            dependencyConfigurationName = UKLIB_API_ELEMENTS_NAME,
            includeIntoProjectStructureMetadata = false,
        )
    )

    /**
     * FIXME: Rewrite this. KotlinJvmTarget creates it's usages (variants) in AbstractKotlinTarget.kotlinComponents and we want to get these
     * usages here or create them using a shared execution path without the copypasted code below when jvm target is not present
     */
    val jvmTarget = project.multiplatformExtension.awaitTargets().singleOrNull { it is KotlinJvmTarget }
    if (jvmTarget != null) {
        val kotlinTargetComponent = jvmTarget.components.single() as KotlinTargetSoftwareComponentImpl
        // FIXME: KT-76687
        @Suppress("UNCHECKED_CAST")
        variants += kotlinTargetComponent.kotlinComponent.internal.usages as Set<DefaultKotlinUsageContext>
    } else {
        // FIXME: Test that stubJvmVariant inherits dependencies from relevant configurations
        val jar = stubJvmJarTask()
        configurations.createConsumable(UKLIB_JAVA_API_ELEMENTS_STUB_NAME) {
            attributes.apply {
                attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(Usage.JAVA_API))
                attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
            }
            inheritCompilationDependenciesFromPublishedCompilations(publishedCompilations.map { it.compilation })
        }
        configurations.createConsumable(UKLIB_JAVA_RUNTIME_ELEMENTS_STUB_NAME) {
            attributes.apply {
                attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(Usage.JAVA_RUNTIME))
                attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
            }
            inheritRuntimeDependenciesFromPublishedCompilations(publishedCompilations.map { it.compilation })
        }
        project.artifacts.add(UKLIB_JAVA_API_ELEMENTS_STUB_NAME, jar) {
            it.extension = "jar"
        }
        project.artifacts.add(UKLIB_JAVA_RUNTIME_ELEMENTS_STUB_NAME, jar) {
            it.extension = "jar"
        }

        variants.addAll(
            listOf(
                DefaultKotlinUsageContext(
                    compilation = anyMetadataCompilation,
                    dependencyConfigurationName = UKLIB_JAVA_API_ELEMENTS_STUB_NAME,
                    includeIntoProjectStructureMetadata = false,
                ),
                DefaultKotlinUsageContext(
                    compilation = anyMetadataCompilation,
                    dependencyConfigurationName = UKLIB_JAVA_RUNTIME_ELEMENTS_STUB_NAME,
                    includeIntoProjectStructureMetadata = false,
                ),
            )
        )
    }

    return variants
}

private fun Project.exposeUmanifestsForInterprojectIdeResolution() {

}

private fun Configuration.inheritCompilationDependenciesFromPublishedCompilations(
    publishedCompilations: List<KotlinCompilation<*>>,
) {
    publishedCompilations.forEach {
        extendsFrom(
            it.internal.configurations.apiConfiguration
        )
        /**
         * If K/N is one of the published compilations, we must promote its implementation dependencies to api
         *
         * FIXME: Remove this extendsFrom after OSIP-667
         */
        if (it is KotlinNativeCompilation) {
            extendsFrom(it.internal.configurations.implementationConfiguration)
        }
    }
}

private fun Configuration.inheritRuntimeDependenciesFromPublishedCompilations(
    publishedCompilations: List<KotlinCompilation<*>>,
) {
    publishedCompilations.forEach {
        it.internal.configurations.runtimeDependencyConfiguration?.let {
            extendsFrom(it)
        }
        /**
         * If K/N is one of the published compilations, we must promote its compile dependencies dependencies to runtime
         *
         * FIXME: Remove this extendsFrom after OSIP-667
         */
        if (it is KotlinNativeCompilation) {
            extendsFrom(it.internal.configurations.compileDependencyConfiguration)
        }
    }
}

private fun Project.stubJvmJarTask(): TaskProvider<Jar> {
    val stubTaskName = "stubJvmJar"
    return tasks.locateTask<Jar>(stubTaskName) ?: tasks.register(stubTaskName, Jar::class.java)
}