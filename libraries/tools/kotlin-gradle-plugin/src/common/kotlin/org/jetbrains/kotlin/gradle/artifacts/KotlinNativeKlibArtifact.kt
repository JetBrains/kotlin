/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.artifacts

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.internal.plugins.DefaultArtifactPublicationSet
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.internal.tasks.ProducesKlib
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationInfo
import org.jetbrains.kotlin.gradle.plugin.KotlinNativeTargetConfigurator.NativeArtifactFormat
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.internal.artifactTypeAttribute
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.UNPACKED_KLIB_VARIANT_NAME
import org.jetbrains.kotlin.gradle.utils.libsDirectory
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.setAttribute
import java.io.File

internal val KotlinNativeKlibArtifact = KotlinTargetArtifact { target, apiElements, _ ->
    if (target !is KotlinNativeTarget) return@KotlinTargetArtifact
    /* Just registering a dummy placeholder that other tasks can use as umbrella */
    val artifactsTask = target.project.registerTask<DefaultTask>(target.artifactsTaskName) {
        it.group = BasePlugin.BUILD_GROUP
        it.description = "Assembles outputs for target '${target.name}'."
    }

    apiElements.outgoing.attributes.setAttribute(target.project.artifactTypeAttribute, NativeArtifactFormat.KLIB)

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

internal fun maybeCreateKlibPackingTask(
    compilation: AbstractKotlinNativeCompilation,
    classifier: String? = null,
    klibProducingTask: TaskProvider<out ProducesKlib>,
): TaskProvider<Zip> {
    return maybeCreateKlibPackingTask(
        compilation,
        classifier,
        klibProducingTask.flatMap { it.klibFile },
        klibProducingTask,
    )
}

internal fun maybeCreateKlibPackingTask(
    compilation: AbstractKotlinNativeCompilation,
    classifier: String? = null,
    klibProvider: Provider<File>,
    producingTask: Any,
): TaskProvider<Zip> {
    val taskName = compilation.disambiguateName(lowerCamelCaseName(classifier, "klib"))
    return compilation.target.project.locateOrRegisterTask<Zip>(taskName) { task ->
        task.from(klibProvider)
        task.destinationDirectory.convention(compilation.target.project.libsDirectory)
        task.archiveExtension.set("klib")
        val klibDisambiguator = compilation.target.disambiguateName(lowerCamelCaseName(classifier, compilation.name))
        task.archiveBaseName.convention("${compilation.project.name}-$klibDisambiguator")
        task.isPreserveFileTimestamps = false
        task.isReproducibleFileOrder = true
        task.dependsOn(producingTask)
    }
}

internal fun createKlibArtifact(
    compilation: AbstractKotlinNativeCompilation,
    classifier: String?,
    klibProducingTask: TaskProvider<out ProducesKlib>,
) {
    val apiElementsName = compilation.target.apiElementsConfigurationName
    val (packTask, packedArtifactFile) = if (compilation.target.project.kotlinPropertiesProvider.produceUnpackedKlibs) {
        val packTask = maybeCreateKlibPackingTask(compilation, classifier, klibProducingTask)
        packTask to packTask.map { it.archiveFile.get().asFile }
    } else {
        klibProducingTask to klibProducingTask.flatMap { it.klibFile }
    }
    with(compilation.project.configurations.getByName(apiElementsName)) {
        val klibArtifact = compilation.project.artifacts.add(name, packedArtifactFile) { artifact ->
            artifact.name = compilation.compilationName
            artifact.extension = "klib"
            artifact.type = "klib"
            artifact.classifier = classifier
            artifact.builtBy(packTask)
        }
        compilation.project.extensions.getByType(DefaultArtifactPublicationSet::class.java).addCandidate(klibArtifact)
        artifacts.add(klibArtifact)
        attributes.setAttribute(compilation.project.artifactTypeAttribute, NativeArtifactFormat.KLIB)

        if (compilation.target.project.kotlinPropertiesProvider.produceUnpackedKlibs) {
            outgoing.variants.getByName(UNPACKED_KLIB_VARIANT_NAME)
                .artifact(klibProducingTask.flatMap { it.klibFile }) {
                    it.builtBy(klibProducingTask)
                }
        }
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
