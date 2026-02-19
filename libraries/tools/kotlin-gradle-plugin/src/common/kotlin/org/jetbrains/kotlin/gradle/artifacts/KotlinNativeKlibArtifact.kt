/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.artifacts

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.internal.tasks.ProducesKlib
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationInfo
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.uklibStateAttribute
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.uklibStateDecompressed
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.uklibViewAttribute
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.KmpPublicationStrategy
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.UKLIB_API_ELEMENTS_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.maybeCreateUklibApiElements
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.uklibFragmentPlatformAttribute
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.libsDirectory
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.maybeCreateConsumable
import org.jetbrains.kotlin.gradle.utils.registerKlibArtifact

internal val KotlinNativeKlibArtifact = KotlinTargetArtifact { target, _, _ ->
    if (target !is KotlinNativeTarget) return@KotlinTargetArtifact
    /* Just registering a dummy placeholder that other tasks can use as umbrella */
    val artifactsTask = target.project.registerTask<DefaultTask>(target.artifactsTaskName) {
        it.group = BasePlugin.BUILD_GROUP
        it.description = "Assembles outputs for target '${target.name}'."
    }

    target.compilations.getByName(MAIN_COMPILATION_NAME).let { mainCompilation ->
        artifactsTask.dependsOn(mainCompilation.compileTaskProvider)
        createRegularKlibArtifact(mainCompilation)
    }
}

internal fun createRegularKlibArtifact(compilation: AbstractKotlinNativeCompilation) = createKlibArtifact(
    compilation = compilation,
    classifier = null,
    klibProducingTask = compilation.compileTaskProvider
)

internal fun AbstractKotlinNativeCompilation.maybeCreateKlibPackingTask(
    classifier: String? = null,
    klibProducingTask: TaskProvider<out ProducesKlib>,
): TaskProvider<Zip> {
    return maybeCreateKlibPackingTask(
        classifier,
        klibProducingTask.map { it.klibDirectory.get() },
    )
}

internal fun AbstractKotlinNativeCompilation.maybeCreateKlibPackingTask(
    classifier: String? = null,
    klibDirectoryProvider: Provider<Directory>,
): TaskProvider<Zip> {
    val taskName = disambiguateName(lowerCamelCaseName(classifier, "klib"))
    return target.project.locateOrRegisterTask<Zip>(taskName) { task ->
        task.from(klibDirectoryProvider)
        task.destinationDirectory.convention(target.project.libsDirectory)
        task.archiveExtension.set("klib")
        val klibDisambiguator = target.disambiguateName(lowerCamelCaseName(classifier, name))
        task.archiveBaseName.convention("${project.name}-$klibDisambiguator")
        task.isPreserveFileTimestamps = false
        task.isReproducibleFileOrder = true
    }
}

internal fun createKlibArtifact(
    compilation: AbstractKotlinNativeCompilation,
    classifier: String?,
    klibProducingTask: TaskProvider<out ProducesKlib>,
) {
    val apiElementsName = compilation.target.apiElementsConfigurationName
    val packedArtifactFile = if (compilation.target.project.kotlinPropertiesProvider.useNonPackedKlibs) {
        // the default artifact should be compressed
        val packTask = compilation.maybeCreateKlibPackingTask(classifier, klibProducingTask)
        packTask.map { it.archiveFile.get().asFile }
    } else {
        klibProducingTask.map { it.klibFile }
    }
    compilation.project.configurations.getByName(apiElementsName)
        .outgoing
        .registerKlibArtifact(packedArtifactFile, compilation.compilationName, classifier)

    when (compilation.project.kotlinPropertiesProvider.kmpPublicationStrategy) {
        KmpPublicationStrategy.UklibPublicationInASingleComponentWithKMPPublication -> {
            val uklibAttribute = compilation.target.uklibFragmentPlatformAttribute.convertToStringForPublicationInUmanifest()
            compilation.project.maybeCreateUklibApiElements().outgoing.variants {
                val variant = it.maybeCreate(uklibAttribute)
                variant.registerKlibArtifact(
                    klibProducingTask.map { it.klibOutput },
                    compilation.compilationName,
                    classifier,
                )
                variant.attributes {
                    it.attribute(uklibStateAttribute, uklibStateDecompressed)
                    it.attribute(uklibViewAttribute, uklibAttribute)
                }
            }
        }
        KmpPublicationStrategy.StandardKMPPublication -> {}
    }
}

internal fun Project.klibOutputDirectory(
    compilation: KotlinCompilationInfo,
): DirectoryProperty {
    val targetSubDirectory = compilation.targetDisambiguationClassifier?.let { "$it/" }.orEmpty()
    return project.objects.directoryProperty().value(
        layout.buildDirectory.dir("classes/kotlin/$targetSubDirectory${compilation.compilationName}")
    )
}
